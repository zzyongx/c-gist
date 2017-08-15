#!/usr/bin/env groovy

import groovy.json.JsonSlurper

def coverage(def json) {
  def total = 0
  def hit = 0
  json.each { category, stats ->
    stats.each { api, stat ->
      total++;
      hit += (stat.req > 0 ? 1 : 0);
    }
  }
  return Math.round(100 * hit/total)
}

def file = new File(this.args[0])
println file.exists() ? coverage(new JsonSlurper().parseText(file.text)) : 0
