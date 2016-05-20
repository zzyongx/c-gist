package lib;

@Grab('mysql:mysql-connector-java:5.1.39')
@GrabConfig(systemClassLoader = true)
@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')
@Grab('org.slf4j:slf4j-simple:1.7.16')
@Grab('com.jayway.jsonpath:json-path:2.2.0')

import groovy.sql.Sql
import groovyx.net.http.URIBuilder
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT
import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.ContentType.URLENC
import com.jayway.jsonpath.JsonPath

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
    
  def g =
    [ debug: true,
      server: null,
      query: [:],
      body: null,
      headers: [:],
      requestContentType: URLENC,
      contentType: TEXT ];
  
  def r = null

  // response
  def resp, respBody

  // expect
  def http, json

  // save
  def cookie = [:]

  // temp object
  def obj = [:]

  def reset() {
    r = g.clone();
    http = [:]
    json = [:]
  }

  def config(Map config) {
    config.each {key, value ->
      g[key] = value
    }
  }

  def http(method, url, config) {
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
    if (r.debug) {
      println "DEBUG REQUEST: ${reqUri}"
      println "DEBUG REQUEST COOKIE: ${http.headers.cookie}"
    }
      
    http.request(method) {
      uri.path = url
      uri.query = r.query
      
      if (method == POST || method == PUT) {
        body = r.body
        requestContentType = r.requestContentType
      }

      response.success = { resp, body ->
        this.resp = resp
        this.respBody = body
      }

      response.failure = { resp ->
        this.resp = resp
      }
    }
  }

  def expect(config) {
    config.delegate = this
    config.call()

    stat.assert++
    if (http.code) assert http.code == resp.status
    else assert 200 == resp.status

    if (json) {
      jsonExpect()
    }
  }


  def jsonExpect() {
    if (r.debug) println("DEBUG RESPONSE: ${respBody}")
    
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
      } else if (value == NotEmpty) {
        assert 0 != ctx.read('$.' + key + ".length()")
      } else if (value instanceof Grep) {
        def closure = value.closure
        this.obj = [:]
        closure.delegate = this
        closure.call()

        assert ctx.read('$.' + key).find {
          obj.every { k, v ->
            it[k] == v
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
  static SQL(def mix) {
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

    Sql.withInstance(url, user, cfg.'jdbc.password', cfg.'jdbc.driver') { db ->
      db.execute(sql)
    }
  }

  static STAT() {
    println "req   : ${bdd.stat.req}"
    println "assert: ${bdd.stat.assert}"
  }

  static NotNull   = { 1 }
  static IsString  = { 2 }
  static IsInteger = { 3 }
  static NotEmpty  = { 10 }

  static class Grep {
    def closure
    Grep(closure) {
      this.closure = closure
    }
  }

  static Grep(closure) {
    return new Grep(closure)
  }
}

