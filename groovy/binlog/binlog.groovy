#!/bin/env groovy

import groovy.sql.Sql

jdbcURL="jdbc:mysql://54.208.78.75/airs"

def printST(e){
  Writer writer = new StringWriter()
  e.printStackTrace(new PrintWriter(writer))
  writer.toString()
}

println "<?xml version='1.0' encoding='UTF-8'?>"
try{mysql=Sql.newInstance(jdbcURL,'userairs', 'userairs', 'com.mysql.jdbc.Driver')}
catch(Exception ex) {
  println "<ROWSET><![CDATA[${printST(ex)}]]></ROWSET>"
  return -999
}

def REQUEST = "pCode=2RH31S101XG01&pStart=200910061523&pStop=201103281305"
if (this.args.size()==1) if (this.args[0]!='') REQUEST =  this.args[0]

def map = [:]
REQUEST.tokenize('&').each{
  def xx=it.tokenize('=')
  map[xx[0]]= xx[1] ?: null
}

def pCode=  map['pCode'] ?: '?'
def pStart="${map['pStart'] ?: '?'}????????????"[0..11]
def pStop="${map['pStop'] ?: '?'}????????????"[0..11]

println "<ROWSET autor='is10-99' versionOf='2014-11-22'>"

println "<!-- ${REQUEST}\n ${jdbcURL}  -->"
if ("$pCode$pStart$pStop".contains('?')){
  println """\
<!-- \
Требуется корректный набор параметров. Пример:
pCode=2RH31S101XG01&pStart=200910061523&pStop=201103281305 \
-->
</ROWSET>
"""
  return -999
}

String sql= "select name, nserv from svod_code where code='$pCode';"
String query= """\
select
concat(substr(tstamp,1,4), '-',
substr(tstamp,5,2), '-',
substr(tstamp,7,2), ' ',
substr(tstamp,9,2), ':',
substr(tstamp,11,2)) tstamp,
value,
case vf when "0" then "" when "1" then "#" end vf,
hand, a_ext
from dx
where
code = '$pCode'
and tstamp between '$pStart' and '$pStop'
order by 1 desc;\
"""

println "<PARAMS pCode='$pCode' pStart='$pStart' pStop='$pStop' />"
println "<interval pStart='${pStart[0..3]}-${pStart[4..5]}-${pStart[6..7]} ${pStart[8..9]}:${pStart[10..11]}' pStop='${pStop[0..3]}-${pStop[4..5]}-${pStop[6..7]} ${pStop[8..9]}:${pStop[10..11]}'/>"

println "<!-- ${sql}  -->"
def srow = mysql.firstRow(sql)
if (!srow){
  println "<signal />\n</ROWSET>"
  return
}

println """<signal>
    <name><![CDATA[${srow[0]}]]></name>
    <nserv><![CDATA[${srow[1]}]]></nserv>
    </signal>"""

println "<!-- ${query}  -->"
try {
  def rr=0
  mysql.rows(query).each{row->
  println "<ROW>"
  row.each {println "<$it.key><![CDATA[$it.value]]></$it.key>"}
    println "</ROW>"
    rr=rr+1
  }
  if(rr==0) println('<ROW />')
} catch(Exception ex) {
  println "<error><![CDATA[${ex.getMessage()}]]></error>"
}
println "</ROWSET>"