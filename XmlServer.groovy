#!/bin/env groovy

import groovy.sql.Sql
import com.sun.net.httpserver.*

def root_Dir= "c:/DROPBOX/edunet/2014/is10-99"
if (getClass().protectionDomain.codeSource.location.path!="/groovy/shell")
  root_Dir =  new File(".").getCanonicalPath()

def jdbc_URL="jdbc:mysql://ec2-54-208-78-75.compute-1.amazonaws.com:3306/airs"
def old_StdOut=System.out

def port= 8000
if(this.args) if(this.args[0].isInteger()) port= this.args[0].toInteger()
def serv = HttpServer.create(new InetSocketAddress(port),0)

serv.with {
  
  createContext("/", new info(rootDir:root_Dir))
  
  HttpContext kill = createContext("/kill", new Kill(server:serv))
  kill.setAuthenticator(new BasicAuthenticator("kill") {
    public boolean checkCredentials(String user, String pwd) {
      println "user==$user pwd==$pwd"
      "$user$pwd"=="is10-99,jhbcsx"
    }
  });
  createContext("/sql2xml/",   new sql2xml(jdbcURL:jdbc_URL))
  createContext("/sql2json/", new sql2json(jdbcURL:jdbc_URL))
  createContext("/html/",   new page(rootDir:root_Dir))
  createContext("/groovy/", new groovy2xml(rootDir:root_Dir,oldStdOut:old_StdOut))
  createContext("/jar",     new groovy2xml(rootDir:root_Dir,oldStdOut:old_StdOut))

  createContext("/crossdomain.xml",   new crossdomain())
  
  setExecutor(null)
  start()
}
println "\nGroovy-xmlServer:$port startded in directory $root_Dir."

class Kill implements HttpHandler {
  def server
  public void handle(HttpExchange exchange) {
  def path = exchange.getRequestURI().getPath()
  println "\nkill::==> ${path}"
    exchange.responseHeaders['Content-Type'] = 'text/html; charset=UTF-8'
    exchange.sendResponseHeaders(200, 0)
    if(path!="/kill") { exchange.close(); return }
  
    def out = new PrintWriter(exchange.getResponseBody())
    out.println """
    <H1>Groovy-xmlServer killed.</H1>
    <hr>
    <H1><a href='/'  target='_self'>Groovy-xmlServer RESTART</a></H1>
    
    """
    out.close()
    exchange.close()
    server.stop(3)
    println "\nGroovy-xmlServer killed.\n-------------------------------"
  }
}

class sql2xml implements HttpHandler {
  def jdbcURL
  public void handle(HttpExchange exchange) {
    def out = new PrintWriter(exchange.getResponseBody())
    def mpath = exchange.getRequestURI().getPath().tokenize("/")
    println "\nsql2xml::==> ${mpath}: "
    def mysql
    try{ mysql=  Sql.newInstance("${jdbcURL}?useUnicode=true&characterEncoding=UTF-8",
      'userairs', 'userairs', 'com.mysql.jdbc.Driver')
    }
    catch(Exception ex) { ex.printStackTrace() }
    
    def query= mpath[1]?: exchange.getRequestURI().getQuery()?:'select 1 one'
    println "$query\n-------------------------------"
    exchange.responseHeaders['Content-Type'] = 'text/xml; charset=UTF-8'
    exchange.responseHeaders['Access-Control-Allow-Origin'] = '*'
    exchange.sendResponseHeaders(200, 0)
    /*-----------------------------------------------*/
    out.println "<?xml version='1.0' encoding='UTF-8'?>"
    out.println "<ROWSET autor='is10-99' versionOf='2015-03-16'>"
    out.println "<SQL><![CDATA[$query]]></SQL>"
    //out.println "<SQL><!-- $query --></SQL>"
    try{
      //mysql.eachRow(query){row->
      mysql.rows(query).each{row->      
        out.println "<ROW>"
        row.each {out.println "<$it.key><![CDATA[$it.value]]></$it.key>"}
        out.println "</ROW>"
      }
    } catch(Exception ex) {
      //ex.printStackTrace();
      def msg = ex.getMessage();
      out.println "<error><![CDATA[${msg}]]></error>"
      println "\nsql2xml: $query\n ${msg}"
    }
    out.println "</ROWSET>"
    /*-----------------------------------------------*/
    
    out.close()
    exchange.close()      
  }
}

class sql2json implements HttpHandler {
  def jdbcURL
  public void handle(HttpExchange exchange) {
    def out = new PrintWriter(exchange.getResponseBody())
    def mpath = exchange.getRequestURI().getPath().tokenize("/")
    println "\nsql2json::==> ${mpath}: "
    def mysql
    try{ mysql=  Sql.newInstance("${jdbcURL}?useUnicode=true&characterEncoding=UTF-8",
      'userairs', 'userairs', 'com.mysql.jdbc.Driver')
    }
    catch(Exception ex) { ex.printStackTrace() }
    exchange.responseHeaders['Content-Type'] = 'application/json; charset=UTF-8'
    exchange.responseHeaders['Access-Control-Allow-Origin'] = '*'
    exchange.sendResponseHeaders(200, 0)
    
    /*-----------------------------------------------*/
    def query
    if(mpath.size()==3){
      query= mpath[2]?:'select 1 one'
      out.print "${mpath[1]}("
    } else 
      query= mpath[1]?:'select 1 one'
    println "$query\n-------------------------------"
    out.print "[{\"autor\":\"is10-99\", \"SQL\":\"$query\"},\n["
    def rr=0
    try {
      mysql.rows(query).each{row->
        if(rr>0) out.print ",{" else out.print "{"; rr=1
        row.each {
          if(rr>1) out.print ","; rr=2
          out.print "\"${it.key}\": \"${it.value}\""
        }
        out.println "}"
      }
      if(mpath.size()==3) out.println "]])"
      else out.println "]]"
    } catch(Exception ex) {
      out.println "], {\"error\":\"${ex.getMessage()}\"}]"
      println "sql2json: $query\n ${ex.getMessage()}"
    }
    /*-----------------------------------------------*/
    
    out.close()
    exchange.close()      
  }
}

class crossdomain implements HttpHandler {
  public void handle(HttpExchange exchange) {
    def out = new PrintWriter(exchange.getResponseBody())
    exchange.responseHeaders['Content-Type'] = 'application/xml; charset=UTF-8'
    exchange.sendResponseHeaders(200, 0)
    println exchange.getRequestURI().getPath()
    
    out.println """<?xml version="1.0"?>

<cross-domain-policy>
   <allow-access-from domain="*" secure="false" />
</cross-domain-policy>"""
    
    out.close()
    exchange.close()
  }
}

class info implements HttpHandler {
  def rootDir
  public void handle(HttpExchange exchange) {
    def out = new PrintWriter(exchange.getResponseBody())
    def path = exchange.getRequestURI().getPath()
    exchange.responseHeaders['Content-Type'] = 'text/html; charset=UTF-8'
    exchange.sendResponseHeaders(200, 0)
    println "\ninfo::==> ${path}: ${path.tokenize("/")}"
   
    if(path =="/"){
      out.println """
<H2>Groovy XML server (16-03-2015):</H2>
<div style="font-size:24">
<ul><li><a href='/crossdomain.xml'  target='_blank'>/crossdomain.xml</a>
<li><b>/sql2.../:</b>
<ul>
<li><a href='/sql2xml/show tables'  target='_blank'>/sql2xml/...</a>
<li><a href='/sql2json/show tables' target='_blank'>/sql2json/...</a>
<li><a href='/sql2json/callback/show tables' target='_blank'>/sql2json/callback/...</a>
</ul>
"""
      out.println "<hr><li><b>/groovy/:</b><ul>"
      new File("${rootDir}/groovy/").eachFileMatch(~/.*.groovy/) { file->
        def fname=file.getName()[0..-8]
        out.println "<li><a href='/groovy/${fname}'  target='_blank'>/groovy/${fname}/...</a>"
      }
      out.println "</ul><li><b>/jar/:</b><ul>"
      new File("${rootDir}/jar/").eachFileMatch(~/.*.jar/) { file->
        def fname=file.getName()[0..-5]
        out.println "<li><a href='/jar/${fname}'  target='_blank'>/jar/${fname}/...</a>"        
      }
      /**/
      out.println "</ul><li><b>/html/:</b><ul>"
      new File("${rootDir}/html/").eachFileMatch(~/.*.html/) { file->
        def fname=file.getName()[0..-6]
        out.println "<li><a href='/html/${fname}'  target='_blank'>/html/${fname}</a>"
      }
      /**/
      out.println """
<hr>
</ul><li><a href='/kill'>/kill</a>
</ul>
</div>
"""
      println "Groovy-xmlServer is ready!"
    }
    out.close()
    exchange.close()
  }
}

class page implements HttpHandler {
  def rootDir
  public void handle(HttpExchange exchange) {
    def mpath = exchange.getRequestURI().getPath().tokenize("/")
    println "\npage::==> ${mpath}: "
    exchange.responseHeaders['Content-Type'] = 'text/html; charset=UTF-8'
    exchange.sendResponseHeaders(200, 0)
    def out = new PrintWriter(exchange.getResponseBody())
    def pageFile= "$rootDir/${mpath[0]}/${mpath[1]}.${mpath[0]}"
    println "out.println new File('$pageFile')"
    out.println new File(pageFile).getText()
    out.close()
    exchange.close()
  }
}

class groovy2xml implements HttpHandler {
  def rootDir
  def oldStdOut
  public void handle(HttpExchange exchange) {
    System.out = oldStdOut
    def mpath = exchange.getRequestURI().getPath().tokenize("/")
    println "\ngroovy2xml::==> ${mpath}: "
    def query= mpath[2]?: exchange.getRequestURI().getQuery()?:''
    
    exchange.responseHeaders['Content-Type'] = 'text/xml; charset=UTF-8'
    exchange.responseHeaders['Access-Control-Allow-Origin'] = '*'
    exchange.sendResponseHeaders(200, 0)
    def out = new PrintWriter(exchange.getResponseBody())

    def groovyFile= "$rootDir/${mpath[0]}/${mpath[1]}.${mpath[0]}"
    println "$groovyFile $query"
    def bufStr = new ByteArrayOutputStream()
    System.out = new PrintStream(bufStr)
    try {
      if(mpath[0]=='groovy'){      
        new GroovyShell().run(new File(groovyFile), query)
      }
      if(mpath[0]=='jar'){
        getClass().classLoader.rootLoader.addURL(new File(groovyFile).toURL())
        Class.forName(mpath[1]).newInstance().main(query)
      }
    } catch(Exception ex) {
      ex.printStackTrace()//? sterr stdout ??? 
      xml = "<error><![CDATA[${ex.getMessage()}]]></error>"
    } 
    System.out = oldStdOut
    def xml= bufStr.toString()
    println "$groovyFile finished.\n-------------------------------"

    out.println xml
    out.close()
    exchange.close()
  }
}

def restart= false;
if(this.args) restart= this.args[-1]=='restart';
if(!restart)
  java.awt.Desktop.getDesktop().browse(new URI("http://localhost:${port}/"));
