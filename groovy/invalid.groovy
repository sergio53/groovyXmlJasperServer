#!/bin/env groovy

import groovy.sql.Sql

if (System.getProperty("os.name")=="Linux"){
  jdbcURL="jdbc:mysql://localhost:3306/airs"
} else {
  jdbcURL="jdbc:mysql://ec2-54-208-78-75.compute-1.amazonaws.com:3306/airs"
}

def mysqlconnection() {
  Sql.newInstance("${jdbcURL}?useUnicode=true&characterEncoding=UTF-8",
    'userairs', 'userairs', 'com.mysql.jdbc.Driver')
}

def printST(e){
  Writer writer = new StringWriter()
  e.printStackTrace(new PrintWriter(writer))
  writer.toString()
}

println "<?xml version='1.0' encoding='UTF-8'?>"
try{sql=mysqlconnection()} catch(Exception ex) {
  println "<ROWSET><![CDATA[${printST(ex)}]]></ROWSET>"
  return -999
}

def REQUEST = "pType=m&pServ=2&pLevel=h2&pStamp=201012240000"
if (this.args.size()==1) if (this.args[0]!='') REQUEST =  this.args[0]

def map = [:]
REQUEST.tokenize('&').each{
  def xx=it.tokenize('=')
  map[xx[0]]= xx[1] != null ? URLDecoder.decode(xx[1]) : null
}

def pType=  map['pType'] ?: '?'
def pServ=  map['pServ'] ?: '?'
def pLevel= map['pLevel'] ?: '?'
def pStamp="${map['pStamp'] ?: '?'}????????????"[0..11]

def a_val= 'a_val'; def a_stat= 'a_stat'
if(pLevel=='h0') {a_val= 'value'; a_stat= 'vf'}
if(pType=='d') a_val= 'a_ext'

String query= """
SELECT s.code, s.name, s.unit, h.$a_val a_val
FROM svod_code s
LEFT OUTER JOIN $pLevel h ON (s.code=h.code)
WHERE
h.${a_stat}='1' AND
s.type='$pType' AND s.nserv=$pServ AND h.tstamp='$pStamp'
ORDER BY 1"""

println "<ROWSET autor='is10-99' jdbcURL='${jdbcURL}'>"
println """<!--${REQUEST}
pType=  $pType
pServ=  $pServ
pStamp= $pStamp
pLevel= $pLevel

$query\
-->"""

def pLevelName= "Исторический уровень: " +
    (pLevel=="h0" ? "1 минута" :
    pLevel=="h1" ? "10 минут" :
    pLevel=="h2" ? "1 час" :
    pLevel=="h3" ? "1 смена" :
    pLevel=="h4" ? "1 сутки" :
    pLevel=="h5" ? "1 месяц" : "НЕИЗВЕСТНО")
def pTypeName= "Тип значений: " +
    (pType=="a" ? "аналоговые сигналы" :
    pType=="m" ? "расчетные значения" :
    pType=="d" ? "дискретные сигналы" : "НЕИЗВЕСТНО")
def pServName= "Группа оборудования: " +
    (pServ=="0" ? "общестанционное" :
    pServ=="1" ? "энергоблок № 1" :
    pServ=="2" ? "энергоблок № 2" :
    pServ=="3" ? "энергоблок № 3" : "НЕИЗВЕСТНО")
def pStampName= pStamp[0..3]+"-"+pStamp[4,5]+"-"+pStamp[6,7]+" "+pStamp[8,9]+":"+pStamp[10,11]

println """<headers>
    <pLevelName><![CDATA[$pLevelName]]></pLevelName>
    <pTypeName><![CDATA[$pTypeName]]></pTypeName>
    <pServName><![CDATA[$pServName]]></pServName>
    <pStampName><![CDATA[$pStampName]]></pStampName>
    </headers>"""

if ("$pType$pServ$pLevel$pStamp".contains('?')){
    println """\
<!--
Требуется корректный набор параметров. Пример:
pType=m&pServ=2&pLevel=h2&pStamp=201104061100
-->
"""
} else
  try {
    def rr=0
    sql.rows(query).each{row->
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