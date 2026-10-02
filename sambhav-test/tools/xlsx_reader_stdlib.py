import zipfile,re,sys,glob,os
import xml.etree.ElementTree as ET
NS={'m':'http://schemas.openxmlformats.org/spreadsheetml/2006/main','r':'http://schemas.openxmlformats.org/officeDocument/2006/relationships'}
def col(ref):
    s=re.match(r'[A-Z]+',ref).group(0);n=0
    for ch in s:n=n*26+ord(ch)-64
    return n-1
class WB:
    def __init__(s,path):
        s.z=zipfile.ZipFile(path)
        s.ss=[]
        if 'xl/sharedStrings.xml' in s.z.namelist():
            for si in ET.fromstring(s.z.read('xl/sharedStrings.xml')).findall('m:si',NS):
                s.ss.append(''.join(t.text or '' for t in si.iter('{%s}t'%NS['m'])))
        wbx=ET.fromstring(s.z.read('xl/workbook.xml'))
        rels={r.get('Id'):r.get('Target') for r in ET.fromstring(s.z.read('xl/_rels/workbook.xml.rels'))}
        s.sheets=[]
        for sh in wbx.find('m:sheets',NS):
            rid=sh.get('{%s}id'%NS['r']);t=rels[rid];t=t if t.startswith('xl/') else 'xl/'+t.lstrip('/')
            s.sheets.append((sh.get('name'),t,sh.get('state')))
    def rows(s,target,limit=None):
        out=[];
        for ev,el in ET.iterparse(s.z.open(target)):
            if el.tag.endswith('}row'):
                r={}
                for c in el.findall('m:c',NS):
                    v=c.find('m:v',NS);t=c.get('t')
                    if t=='inlineStr': val=''.join(x.text or '' for x in c.iter('{%s}t'%NS['m']))
                    elif v is None: continue
                    elif t=='s': val=s.ss[int(v.text)]
                    else: val=v.text
                    r[col(c.get('r'))]=val
                out.append((int(el.get('r')),r));el.clear()
                if limit and len(out)>=limit: break
        return out
if __name__=='__main__':
    base='data2/Sambhav'
    for f in sorted(glob.glob(base+'/*/*.xlsx')):
        wb=WB(f);print('\n#####',f.replace(base+'/',''),os.path.getsize(f)//1024,'KB')
        for name,t,st in wb.sheets:
            rows=wb.rows(t)
            ncol=max([max(r) for _,r in rows if r] or [0])+1
            print('  sheet',repr(name),st or '','| rows',len(rows),'| cols',ncol)
