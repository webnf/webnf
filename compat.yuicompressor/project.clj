(defproject webnf.compat/yuicompressor "2.4.8"
  :description "Avoid repackaging of rhino in yuicompressor
    repackaging of com.yahoo.platform.yui/yuicompressor"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[org.mozilla/rhino "_" :upgrade false]])

