def jdbcURL="jdbc:mysql://54.208.78.75:3306/airs"

def REQUEST = "pLev=1&pNumber=0111&pH=h2&pStart=201012240000&pStop=201012242300"
if (this.args.size()==1) if (this.args[0]!='') REQUEST =  this.args[0]

def map = [:]
REQUEST.tokenize('&').each{
  def xx=it.tokenize('=')
  map[xx[0]]= xx[1]
}

def pLev=  map['pLev']
def pNumber=  map['pNumber']
def pStart=map['pStart']
def pStop=map['pStop']
def pH=map['pH']

String sql= "SELECT level,number,name,descr,content1 FROM reports1 WHERE lev=$pLev AND number='$pNumber'  ORDER BY 2"
println """<?xml version='1.0' encoding='UTF-8'?>
<ROWSET pLev='$pLev' pNumber='$pNumber' pH='$pH' pStart='$pStart' pStop='$pStop'>
<!-- ${sql}  -->
"""
import groovy.sql.Sql
def mysql = Sql.newInstance(jdbcURL,'userairs', 'userairs', 'com.mysql.jdbc.Driver')
def row = mysql.firstRow(sql)
println "<content1 level='" +row[0]+ "' number='" +row[1]+ "' name='" +row[2]+
  "' descr='" +row[3]+ "' >\n<![CDATA[\n" +row[4]+ "\n]]>"

def cont1= row[4].replace(';',' ; ').tokenize(';')
print "<!--"
for(int x0=0; x0<13; x0++){
  for(int x1=0; x1<11; x1++) print("${cont1[11*x0+x1]}".trim()+'; ')
  println ''
}
println "-->"

def codeArr= []
def content1= []
def codeList= ""
for(int x0=0; x0<13; x0++){
  def x1= x0*11
  content1[x0]= ['ord':cont1[x1], 'code':cont1[x1+1].trim(), 'name':cont1[x1+2], 'unit':cont1[x1+3] ]
  if (codeList!="") codeList = "$codeList, ";  codeList = "${codeList}'${content1[x0]['code']}'"
  codeArr[x0]= content1[x0]['code']
  println "<signal${x0} ord='" + content1[x0]['ord'] +
            "' code='"  + (content1[x0]['code'] ?: '') +
            "' >\n" +
  "<name><![CDATA["  + (content1[x0]['name'] ?: '') + "]]></name>\n" +
  "<unit><![CDATA["  + (content1[x0]['unit'] ?: '') + "]]></unit>\n" +
  "</signal${x0}>"
}
println "</content1>"

String stmt= """SELECT tstamp,code,a_val,a_stat,type,a_ext FROM $pH
WHERE tstamp BETWEEN '$pStart' AND '$pStop' AND
code in ($codeList)
"""

println "<!-- $stmt -->"
def result= [:]
def dInt
import groovy.time.TimeCategory
use (TimeCategory) {
  dInt= [
    'h0': 1.minute,
    'h1': 10.minute,
    'h2': 1.hour,
    'h3': 12.hour,
    'h4': 1.day,
    'h5': 1.month
  ]
}
def dt= dInt[pH]

def fmt =  new java.text.SimpleDateFormat("yyyyMMddHHmm")
def t1 = fmt.parse(pStart)
def t2 = fmt.parse(pStop)

print "<!--"
def t1s
while(t1<=t2){
  t1s=fmt.format(t1)
  println t1s
  def empty= ['','-','-','-','-','-','-','-','-','-','-','-','-','-']
  switch (pH) {
  case 'h2':
    empty[0]="${t1s[6,7]}/${t1s[8,9]}"
    break;
  default:
    empty[0]= t1s
  }
  result[t1s]=empty
  use (TimeCategory) {t1=t1+dt}
}
println "-->"

print "<!--"
mysql.rows(stmt).each{row1 ->
  println row1
  codeArr.eachWithIndex{it,ix->
    if(it==row1['code']){
      result[row1['tstamp']][ix+1]=row1['a_val']
    }
  }
}
println "-->"

print "<!--"
result.each{k,v->println "$k:$v"}
println "-->"

result.each{k, v ->
  print "<ROW tstamp='$k'" + " time='" + v[0]+ "'"
  for(int i1=1; i1<v.size(); i1++)  print " val$i1='" + v[i1]+ "'"
  println " />"
}


println "</ROWSET>"