#!/usr/bin/env python3
"""Run existing calendar, TLS, diagnostic and API7 wake-lock regressions."""
from pathlib import Path
import os
import subprocess

root = Path(__file__).resolve().parents[1]
separator = os.pathsep
libraries = ['ical4j', 'commons-logging', 'commons-codec', 'commons-lang',
             'backport', 'sc-core', 'sc-prov', 'sc-tls']
classpath = separator.join(['official/classes', 'resources', 'deps/android-platform7.jar'] +
                           ['deps/' + name + '.jar' for name in libraries])
output = root / 'official/test-classes'
output.mkdir(exist_ok=True)
names = ['CalendarTest', 'DstTest', 'LegacyZoneTest', 'AgendaWindowTest', 'DiagnosticPrivacyTest']
sources = ['tests/' + name + '.java' for name in names] + [
    'tests-111/CertificatePathTest.java', 'tests-111/WakeLockApi7Test.java']
def run(arguments):
    subprocess.run(arguments, cwd=root, check=True)
run(['java', '-jar', 'deps/ecj.jar', '-source', '1.6', '-target', '1.6', '-nowarn',
     '-bootclasspath', 'deps/android-platform7.jar', '-classpath', classpath,
     '-d', str(output)] + sources)
for name in names + ['CertificatePathTest', 'WakeLockApi7Test']:
    run(['java', '-cp', str(output) + separator + classpath, 'cl.balocchi.nookpanel.' + name])
run(['java', '-jar', 'deps/ecj.jar', '-source', '1.6', '-target', '1.6', '-nowarn',
     '-bootclasspath', 'deps/android-platform7.jar', '-classpath', 'deps/apksig.jar',
     '-d', str(output), 'tests/VerifyApk.java'])
for apk in ['official/Panel-NOOK.apk', 'releases/Panel-NOOK-1.1.3.apk']:
    run(['java', '-cp', str(output) + separator + 'deps/apksig.jar', 'VerifyApk', apk])
print('All listed regression and API7 signature checks passed. Hardware/battery tests are separate.')
