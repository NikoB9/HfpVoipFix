"""Exercise boot guard script decisions without mounting or accessing a device."""
from pathlib import Path
import tempfile,subprocess,os
base=Path(__file__).resolve().parents[1]/'native-runtime'
fp='Redmi/begonia/begonia:11/RP1A.200720.011/V12.5.6.0.RGGIDXM:user/release-keys'
orig='6ca4f18a2652f5ecd0faa7e151f1637e4943da7bfe4256de72116b5c51b1b809'
patched='3387c3b7e9bc593728cd13c20e911007befb8fa25ea1dc8338645a487ec09ddc'
xml='2f5f9e71334418d643b7441ba8948bf21c2f4032b81244a60563b64cab37e5da'
prefix='''getprop(){ printf '%s' "$TEST_FP"; }
log(){ :; }
setprop(){ printf '%s' "$2" > "$MODDIR/marker"; }
sha256sum(){ case "$1" in "$MODDIR"/*) printf '%s file' "$TEST_PAYLOAD";; */audio_policy_configuration.xml) printf '%s file' "$TEST_XML";; *) printf '%s file' "$TEST_HAL";; esac; }
'''
count=0
with tempfile.TemporaryDirectory() as td:
 t=Path(td)
 def run(script,**changes):
  global count
  env=dict(os.environ,TEST_FP=fp,TEST_HAL=orig,TEST_XML=xml,TEST_PAYLOAD=patched);env.update(changes)
  p=t/script;p.write_text((base/script).read_text().replace('MODDIR=${0%/*}','MODDIR=${0%/*}\n'+prefix))
  subprocess.run(['sh',str(p)],env=env,check=True);count+=1
 for hal in (orig,patched):
  run('post-fs-data.sh',TEST_HAL=hal);assert not (t/'disable').exists() and not (t/'skip_mount').exists()
 for key in ('TEST_FP','TEST_HAL','TEST_XML','TEST_PAYLOAD'):
  run('post-fs-data.sh',**{key:'wrong'});assert (t/'disable').exists() and (t/'skip_mount').exists();(t/'disable').unlink();(t/'skip_mount').unlink()
 run('service.sh',TEST_HAL=patched);assert (t/'marker').read_text()=='1.9.0-persistent' and not (t/'disable').exists()
 run('service.sh',TEST_HAL='wrong');assert (t/'disable').exists() and (t/'marker').read_text()=='runtime-inactive'
print('PASS',count,'native runtime boot/service guard fixtures; no device or mounting test')
