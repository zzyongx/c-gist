package lib;

@GrabConfig(systemClassLoader = true)
@Grab('mysql:mysql-connector-java:5.1.39')
@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')
@GrabExclude('xml-apis:xml-apis')
@Grab('com.jayway.jsonpath:json-path:2.2.0')
/* we must provide a slf4j implement for jsonpath */
@Grab('org.slf4j:slf4j-simple:1.7.16')
@Grab('org.apache.httpcomponents:httpmime:4.5.2')

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.sql.Sql
import groovyx.net.http.URIBuilder
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT
import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.ContentType.URLENC
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.ContentType
import org.apache.http.conn.EofSensorInputStream
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException

class JsonReader {
  def ctx
  def JsonReader(ctx) {
    this.ctx = ctx;
  }
    
  def propertyMissing(String name) {
    ctx.read('$.' + name)
  }
}

class BDD {
  def stat = [req: 0, assert: 0]

  def coverKey  
  def cover = [:]
    
  def g =
    [ debug: true,
      server: null,
      query: [:],
      body: null,
      multi: null,
      headers: [:],
      requestContentType: URLENC,
      contentType: TEXT ];
  
  def r = null

  // response
  def resp, respBody

  // expect
  def http, json, string

  // save
  def cookie = [:]

  // temp object
  def obj = [:]

  def reset() {
    r = g.clone()
    http = [:]
    json = [:]
    string = null
  }

  def config(Map config) {
    config.each {key, value ->
      g[key] = value
    }
  }

  def coverKey(method, url) {
    if (url instanceof String) {
      return "$method:$url";
    } else {
      def nurl = String.join("{}", url.getStrings());
      return "$method:$nurl";
    }
  }

  def coverAddReq() {
    if (cover[coverKey]) {
      cover[coverKey].req++
    } else {
      cover[coverKey] = [req: 1, assert: 0]
    }
  }
  
  def coverAddAssert() {
    cover[coverKey].assert++;
  }

  def http(method, url, config) {
    coverKey = coverKey(method, url)
    reset()

    if (config) {
      config.delegate = this
      config.call()
    }

    def http = new HTTPBuilder(r.server)
    http.headers = [*:g.headers, *:r.headers]

    if (!http.headers.cookie) {
      cookie.each {key, value ->
        http.headers["cookie"] = key + "=" + value
      }
    }

    URIBuilder reqUri = new URIBuilder(url)
    reqUri.query = r.query    

    if (r.body instanceof String ||
        r.body instanceof GString ||
        r.body instanceof byte[]) {
      r.requestContentType = 'application/octet-stream'
    } else if (r.body && reqUri.query) {
      reqUri.query << r.body
    }

    stat.req++;
    coverAddReq();
    
    if (r.debug) {
      println "DEBUG REQUEST: $method ${reqUri}"
      if (reqUri.query) println "DEBUG REQUEST QUERY: ${reqUri.query}"
      if (r.body && r.requestContentType != 'application/octet-stream') {
        println "DEBUG REQUEST BODY: ${r.body}"
      }
      println "DEBUG REQUEST COOKIE: ${http.headers.cookie}"
    }
      
    http.request(method) { req ->
      uri.path = url
      uri.query = r.query
      
      if (method == POST || method == PUT) {
        if (r.multi) {
          def builder = MultipartEntityBuilder.create();
          builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
          r.multi.each {k, v ->
            if (v instanceof Map) {
              builder.addBinaryBody(k, v.file, ContentType.DEFAULT_BINARY, v.filename)
            } else {
              builder.addTextBody(k, v);
            }
          }
          req.setEntity(builder.build());
          requestContentType = "multipart/form-data"
        } else {
          body = r.body
          requestContentType = r.requestContentType
        }
      }

      response.success = { resp, body ->
        this.resp = resp
        if (body instanceof Reader) {
          this.respBody = body.text
        } else if (body instanceof EofSensorInputStream) {
          this.respBody = null
        } else {
          this.respBody = body
        }
      }

      response.failure = { resp ->
        this.resp = resp
        this.respBody = null
      }
    }
    if (r.debug) println("DEBUG RESPONSE: ${respBody}")
  }

  def expect(config) {
    config.delegate = this
    config.call()
    
    stat.assert++
    coverAddAssert()

    if (http.code) assert http.code == resp.status
    else assert 200 == resp.status

    if (string) {
      stringExpect();
    } else if (json) {
      jsonExpect()
    }
  }

  def stringExpect() {
    assert string == respBody
  }

  def jsonExpect() {
    def ctx = JsonPath.parse(respBody)
    assert null != ctx
    
    json.each {key, value ->
      stat.assert++;

      if (value == NotNull) {
        assert null != ctx.read('$.' + key)
      } else if (value == IsString) {
        assert ctx.read('$.' + key) instanceof String
      } else if (value == IsInteger) {
        def actual = ctx.read('$.' + key)
        assert actual instanceof Integer || actual instanceof Long
      } else if (value == NotFound) {
        try {
          assert null == ctx.read('$.' + key)
        } catch (PathNotFoundException e) {}
      } else if (value == NotEmpty) {
        assert 0 != ctx.read('$.' + key + ".length()")
      } else if (value instanceof Grep) {
        def closure = value.closure
        this.obj = [:]
        closure.delegate = this
        closure.call()
        
        assert ctx.read('$.' + key).find {
          def ctxs = JsonPath.parse(it)
          
          obj.every { k, v ->
            try {
              ctxs.read('$.' + k) == v
            } catch (PathNotFoundException e) {}
          }
        }
        
      } else if (key == 'closure') {
        value.call(new JsonReader(ctx))
      } else {
        assert value == ctx.read('$.' + key)
      }
    }
  }

  def save(config) {
    config.delegate = this
    config.call()
  }

  static bdd = new BDD();

  static CONFIG(option) {
    bdd.config(option)
  }

  static GET(url, config = null) {
    bdd.http(GET, url, config)
  }

  static POST(url, config = null) {
    bdd.http(POST, url, config)
  }

  static PUT(url, config = null) {
    bdd.http(PUT, url, config)
  }

  static DELETE(url, config = null) {
    bdd.http(DELETE, url, config)
  }

  static EXPECT(config) {
    bdd.expect(config)
  }

  static SAVE(config) {
    bdd.save(config)
  }

  static byte[] F(file) {
    return new File(".", file).bytes;
  }

  static DEVCFG() {
    def dir = '../src/main/resources'
    def properties = new Properties()
    def files = ['application-default.properties',
                 'application-dev.properties',
                 'application-test.properties']
    files.each { file ->
      try {
        new File(dir, file).withInputStream {
          properties.load(it)
        }
      } catch(e) {}
    } 

    return properties
  }

  static SQL_CFG = DEVCFG();
  static SQLCONFIG(def map) {
    map.each {key, value ->
      if (key == 'user') SQL_CFG.'jdbc.username' = value
      else if (key == 'db') {
        SQL_CFG.'jdbc.url' = SQL_CFG.'jdbc.url'.replaceAll(/[^\/]+\?/, "$value?")
      }
    }
  }

  /* all test database must have same password */
  static SQL(def mix, def closure = null) {
    def cfg = SQL_CFG
    def user = cfg.'jdbc.username'
    def url = cfg.'jdbc.url'
    def sql
    
    if (mix instanceof Map) {
      user = mix.user ?: user
      sql = mix.sql
      if (mix.db) {
        url = url.replaceAll(/[^\/]+\?/, "${mix.db}?")
      }
    } else {
      sql = mix
    }

    def password = cfg.'jdbc.password', driver = cfg.'jdbc.driver'
    println "DEBUG SQL: $sql"

    if (closure) {
      Sql.withInstance(url, user, password, driver) { db ->
        db.eachRow(sql, closure)
      }
    } else {
      Sql.withInstance(url, user, password, driver) { db ->
        db.execute(sql)
      }
    }
  }

  static STAT() {
    println "req   : ${bdd.stat.req}"
    println "assert: ${bdd.stat.assert}"
    COVER()
  }

  static NotNull   = { 1 }
  static IsString  = { 2 }
  static IsInteger = { 3 }
  static NotEmpty  = { 10 }
  static NotFound  = { 11 }

  static class Grep {
    def closure
    Grep(closure) {
      this.closure = closure
    }
  }

  static Grep(closure) {
    return new Grep(closure)
  }

  static COVER() {
    def viewFile = new File(".", "bdd.cover.json")
    def view = [:]
    if (viewFile.exists()) {
      view = new JsonSlurper().parseText(viewFile.text)
    } else {
      bdd.GET("/jsondoc") {
        r.debug = false
      }
      bdd.respBody.apis[""].each { api ->
        view[api.name] = [:]
        api.methods.each { method ->
          def verb = method.verb[0]
          def path = method.path[0]
          def nurl = path.replaceAll(/\{.+?\}/, '{}')

          view[api.name]["$verb:$nurl"] = [method: verb, path: path, req: 0, assert: 0]
        }
      }
    }

    bdd.cover.each { k, v ->
      view.each {name, map ->
        if (map[k]) {
          map[k].req += v.req
          map[k].assert += v.assert
        }
      }
    }

    viewFile.write(JsonOutput.toJson(view))
  }
}

