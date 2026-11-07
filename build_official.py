#!/usr/bin/env python3
"""API7 build: ECJ Java6, Android dx1.7, official AAPT2 and official apksig."""
from pathlib import Path
import subprocess as sp,zipfile,hashlib,shutil,json,io,os
R=Path(__file__).resolve().parent
O=R/'official'
if not (R/'signing/key.pem').is_file() or not (R/'signing/cert.pem').is_file():
 raise SystemExit('Missing local signing key: run python3 tools/create_signing_key.py first. Never commit signing/.')
def run(args):sp.run([str(x)for x in args],cwd=R,check=True)
libs=['sc-core','sc-prov','sc-tls','ical4j','commons-logging','commons-codec','commons-lang','backport']
cp=':'.join('deps/'+n+'.jar'for n in libs)
shutil.rmtree(O/'classes',ignore_errors=True);(O/'classes').mkdir()
run(['java','-jar','deps/ecj.jar','-source','1.6','-target','1.6','-nowarn','-bootclasspath','deps/android-platform7.jar','-classpath',cp,'-d','official/classes']+sorted(str(p.relative_to(R))for p in (R/'src').rglob('*.java')))
with zipfile.ZipFile(O/'app-classes.jar','w',zipfile.ZIP_DEFLATED)as z:
 for p in (O/'classes').rglob('*.class'):z.write(p,p.relative_to(O/'classes'))
run(['java','-Xmx1g','-cp','deps/dx.jar','com.android.dx.command.Main','--dex','--output=official/classes.dex','official/app-classes.jar']+['deps/'+n+'.jar'for n in libs])
assert (O/'classes.dex').read_bytes()[:8]==b'dex\n035\0'
aapt=R/'deps/aapt2'
if not aapt.exists():
 with zipfile.ZipFile(R/'deps/aapt2-linux.jar')as z:aapt.write_bytes(z.read('aapt2'))
 aapt.chmod(0o755)
run([aapt,'link','-I','deps/android-platform7.jar','--manifest','official/AndroidManifest.xml','-A','assets','--min-sdk-version','7','--target-sdk-version','7','--no-auto-version','-o','official/base.apk'])
files={'classes.dex':(O/'classes.dex').read_bytes()}
for name in libs:
 with zipfile.ZipFile(R/('deps/'+name+'.jar'))as z:
  for n in z.namelist():
   if not n.endswith(('/','.class'))and not n.startswith('META-INF/'):files.setdefault(n,z.read(n))
for p in (R/'resources').rglob('*'):
 if p.is_file():files[str(p.relative_to(R/'resources'))]=p.read_bytes()
files['ical4j.properties']=(R/'assets/ical4j.properties').read_bytes()
files['assets/THIRD-PARTY.txt']=(R/'THIRD-PARTY.txt').read_bytes()
shutil.copyfile(O/'base.apk',O/'unsigned.apk')
with zipfile.ZipFile(O/'unsigned.apk','a',zipfile.ZIP_DEFLATED)as z:
 for n,b in sorted(files.items()):z.writestr(n,b)
run(['java','-jar','deps/ecj.jar','-source','1.6','-target','1.6','-nowarn','-bootclasspath','deps/android-platform7.jar','-classpath','deps/apksig.jar','-d','official/sign-classes','official/SignApk.java'])
run(['openssl','pkcs8','-topk8','-nocrypt','-in','signing/key.pem','-outform','DER','-out','official/signing-key.pk8'])
run(['java','-cp','official/sign-classes:deps/apksig.jar','SignApk','official/unsigned.apk','official/Panel-NOOK.apk','official/signing-key.pk8','signing/cert.pem'])
signed=(O/'Panel-NOOK.apk').read_bytes()
with zipfile.ZipFile(io.BytesIO(signed))as z:assert z.testzip()is None
with open(O/'Panel-NOOK.stable.apk','wb')as f:f.write(signed);f.flush();os.fsync(f.fileno())
os.replace(O/'Panel-NOOK.stable.apk',O/'Panel-NOOK.apk')
print('APK', (O/'Panel-NOOK.apk').stat().st_size,hashlib.sha256((O/'Panel-NOOK.apk').read_bytes()).hexdigest())
