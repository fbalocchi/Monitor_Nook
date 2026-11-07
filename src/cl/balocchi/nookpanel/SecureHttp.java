package cl.balocchi.nookpanel;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import org.spongycastle.tls.*;
import org.spongycastle.tls.crypto.TlsCertificate;
import org.spongycastle.tls.crypto.impl.bc.BcTlsCrypto;

/** TLS 1.2 for API 7. The SpongyCastle integration follows the MIT licensed
 * TRMNL legacy NOOK client (copyright 2026 BPMCT); see THIRD-PARTY.txt. */
public class SecureHttp {
    private final Provider provider = new org.spongycastle.jce.provider.BouncyCastleProvider();
    private final Set<TrustAnchor> roots = new HashSet<TrustAnchor>();
    /** Public peer-certificate metadata only; never a request URL or body. */
    public String certificateTrace="";
    public SecureHttp(byte[] bundle) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509", provider);
        Collection<?> certs = cf.generateCertificates(new ByteArrayInputStream(bundle));
        for (Object c : certs) roots.add(new TrustAnchor((X509Certificate)c, null));
        if (roots.isEmpty()) throw new IOException("Sin certificados de confianza");
    }
    private void validate(TlsServerCertificate input, String host) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509", provider);
        List<X509Certificate> chain = new ArrayList<X509Certificate>();
        for (TlsCertificate c : input.getCertificate().getCertificateList()) {
            chain.add((X509Certificate)cf.generateCertificate(new ByteArrayInputStream(c.getEncoded())));
        }
        validateChain(chain,host);
    }
    void validateChain(List<X509Certificate> chain,String host) throws Exception {
        if (chain.isEmpty()) throw new CertificateException("Cadena vacia");
        StringBuilder trace=new StringBuilder("Peer: ");trace.append(host).append("\nCertificates: ").append(chain.size()).append('\n');
        for(int i=0;i<Math.min(8,chain.size());i++){
            X509Certificate cert=chain.get(i);byte[] digest=MessageDigest.getInstance("SHA-256",provider).digest(cert.getEncoded());
            trace.append(i).append(" subject=").append(publicName(cert.getSubjectDN().getName())).append(" issuer=").append(publicName(cert.getIssuerDN().getName())).append(" sha256=");
            for(byte b:digest){int v=b&255;trace.append("0123456789abcdef".charAt(v>>>4)).append("0123456789abcdef".charAt(v&15));}
            trace.append(" valid=").append(cert.getNotBefore().getTime()).append("..").append(cert.getNotAfter().getTime()).append('\n');
        }
        certificateTrace=trace.toString();
        X509Certificate leaf = chain.get(0);
        X509CertSelector target=new X509CertSelector();target.setCertificate(leaf);
        PKIXBuilderParameters p = new PKIXBuilderParameters(roots,target);
        p.setRevocationEnabled(false);
        p.setMaxPathLength(6);
        // Build a valid path to our existing trust store. A server may append
        // a cross-signed root whose issuer is absent even when the modern root
        // itself is trusted. Validating the entire supplied list rejects it.
        p.addCertStore(CertStore.getInstance("Collection",new CollectionCertStoreParameters(chain),provider));
        PKIXCertPathBuilderResult result=(PKIXCertPathBuilderResult)CertPathBuilder.getInstance("PKIX",provider).build(p);
        certificateTrace+="PKIX verified, path certificates="+result.getCertPath().getCertificates().size()+"\n";
        List<String> usage=leaf.getExtendedKeyUsage();
        if (usage!=null && !usage.contains("1.3.6.1.5.5.7.3.1") && !usage.contains("2.5.29.37.0"))
            throw new CertificateException("Certificado no autorizado para servidor");
        Collection<List<?>> names=leaf.getSubjectAlternativeNames();
        boolean match=false;
        if (names!=null) for (List<?> name : names) {
            if (((Integer)name.get(0)).intValue()==2 && matches(host,String.valueOf(name.get(1)))) match=true;
        }
        if (!match) throw new CertificateException("Nombre del servidor no coincide");
    }
    private static String publicName(String name){name=name.replace('\n',' ').replace('\r',' ');return name.length()>384?name.substring(0,384):name;}
    static boolean matches(String h,String pattern) {
        h=h.toLowerCase(Locale.US); pattern=pattern.toLowerCase(Locale.US);
        if (h.equals(pattern)) return true;
        if (!pattern.startsWith("*.")) return false;
        String suffix=pattern.substring(1);
        if (!h.endsWith(suffix)) return false;
        String left=h.substring(0,h.length()-suffix.length());
        return left.length()>0 && left.indexOf('.')<0;
    }
    private DefaultTlsClient client(final String host) {
        return new DefaultTlsClient(new BcTlsCrypto(new SecureRandom())) {
            public ProtocolVersion getClientVersion() {return ProtocolVersion.TLSv12;}
            public ProtocolVersion getMinimumVersion() {return ProtocolVersion.TLSv12;}
            public int[] getCipherSuites() {return new int[]{
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384};}
            @SuppressWarnings("unchecked")
            public Hashtable getClientExtensions() throws IOException {
                Hashtable ex=TlsExtensionsUtils.ensureExtensionsInitialised(super.getClientExtensions());
                Vector names=new Vector(); names.addElement(new ServerName(NameType.host_name,host));
                TlsExtensionsUtils.addServerNameExtension(ex,new ServerNameList(names));
                TlsExtensionsUtils.addExtendedMasterSecretExtension(ex);
                return ex;
            }
            public TlsAuthentication getAuthentication() {
                return new TlsAuthentication() {
                    public void notifyServerCertificate(TlsServerCertificate c) throws IOException {
                        try {validate(c,host);} catch(Exception ex) {
                            // Do not log URLs or calendar contents, including exception messages.
                            throw new TlsFatalAlert(AlertDescription.bad_certificate,ex);
                        }
                    }
                    public TlsCredentials getClientCredentials(CertificateRequest r) {return null;}
                };
            }
        };
    }
    protected Socket openSocket(String host)throws IOException {Socket s=new Socket();s.connect(new InetSocketAddress(host,443),20000);return s;}
    public byte[] get(String address,int limit) throws Exception {return get(new URL(address),limit,0);}
    private byte[] get(URL u,int limit,int redirects) throws Exception {
        if (!"https".equals(u.getProtocol()) || u.getUserInfo()!=null || u.getPort()>0 && u.getPort()!=443)
            throw new IOException("Solo se permite HTTPS");
        String host=u.getHost();
        if (!host.equals("api.open-meteo.com") && !host.equals("calendar.google.com"))
            throw new IOException("Servidor no permitido");
        certificateTrace="Peer: "+host+"\nHandshake pending\n";
        Socket socket=openSocket(host); TlsClientProtocol tls=null;
        try {
            socket.setSoTimeout(20000);
            tls=new TlsClientProtocol(socket.getInputStream(),socket.getOutputStream());
            tls.connect(client(host));
            OutputStream out=tls.getOutputStream();
            String path=u.getFile(); if(path.length()==0)path="/";
            if(path.indexOf('\r')>=0||path.indexOf('\n')>=0)throw new IOException("Ruta invalida");
            out.write(("GET "+path+" HTTP/1.1\r\nHost: "+host+"\r\nUser-Agent: NookPanel/1.0\r\nAccept: */*\r\nAccept-Encoding: identity\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));out.flush();
            InputStream in=tls.getInputStream();String status=line(in);
            String[] parts=status.split(" ",3);if(parts.length<2)throw new IOException("Respuesta HTTP invalida");
            int code=Integer.parseInt(parts[1]);Map<String,String> headers=new HashMap<String,String>();
            int headerSize=0;
            for(;;){String s=line(in);if(s.length()==0)break;headerSize+=s.length();if(headerSize>32768)throw new IOException("Cabeceras grandes");int k=s.indexOf(':');if(k>0)headers.put(s.substring(0,k).toLowerCase(Locale.US),s.substring(k+1).trim());}
            if(code>=300&&code<=399){
                if(redirects>=3||!headers.containsKey("location"))throw new IOException("Redireccion invalida");
                URL next=new URL(u,headers.get("location"));
                if(!next.getHost().equals(host)||!next.getProtocol().equals("https"))throw new IOException("Redireccion no permitida");
                // Close current TLS/socket before starting another connection.
                try {tls.close();} catch(IOException ignored){} socket.close();
                return get(next,limit,redirects+1);
            }
            if(code!=200)throw new IOException("HTTP "+code);
            String encoding=headers.get("content-encoding");
            if(encoding!=null&&!encoding.equalsIgnoreCase("identity"))throw new IOException("Compresion no solicitada");
            ByteArrayOutputStream data=new ByteArrayOutputStream();
            if("chunked".equalsIgnoreCase(headers.get("transfer-encoding"))){
                for(;;){String size=line(in);int k=size.indexOf(';');if(k>=0)size=size.substring(0,k);long n=Long.parseLong(size.trim(),16);if(n==0)break;if(n<0||n>limit-data.size())throw new IOException("Datos demasiado grandes");copy(in,data,(int)n);if(!line(in).equals(""))throw new IOException("Fragmento invalido");}
            }else if(headers.containsKey("content-length")){
                long n=Long.parseLong(headers.get("content-length"));if(n<0||n>limit)throw new IOException("Datos demasiado grandes");copy(in,data,(int)n);
            }else{
                byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(n>limit-data.size())throw new IOException("Datos demasiado grandes");data.write(b,0,n);}
            }
            return data.toByteArray();
        }finally {if(tls!=null)try{tls.close();}catch(IOException ignored){}try{socket.close();}catch(IOException ignored){}}
    }
    static String line(InputStream in)throws IOException{
        ByteArrayOutputStream s=new ByteArrayOutputStream();int b;
        while((b=in.read())!=-1){if(b==10)return new String(s.toByteArray(),"US-ASCII").replace("\r","");if(s.size()>=8192)throw new IOException("Linea larga");s.write(b);}
        throw new EOFException("Respuesta incompleta");
    }
    static void copy(InputStream in,OutputStream out,int count)throws IOException{
        byte[] buf=new byte[4096];while(count>0){int n=in.read(buf,0,Math.min(buf.length,count));if(n<0)throw new EOFException("Respuesta incompleta");out.write(buf,0,n);count-=n;}
    }
}
