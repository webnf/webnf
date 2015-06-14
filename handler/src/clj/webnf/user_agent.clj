(ns webnf.user-agent
  (:import eu.bitwalker.useragentutils.UserAgent))

(defn parse-user-agent [ua]
  (let [a (UserAgent/parseUserAgentString ua)
        b (.getBrowser a)
        bv (.getBrowserVersion a)
        os (.getOperatingSystem a)]
    (cond-> {}
      os (assoc :operating-system
                {:name (.getName os)
                 :device-type (-> os .getDeviceType .getName)
                 :group (-> os .getGroup .getName)
                 :manufacturer (-> os .getManufacturer .getName)
                 :mobile (-> os .isMobileDevice)})
      b  (assoc :browser
                (cond-> {:name (.getName b)
                         :type (-> b .getBrowserType .getName)
                         :manufacturer (-> b .getManufacturer .getName)
                         :rendering-engine (-> b .getRenderingEngine .name)}
                  bv (assoc :version (.getVersion bv)))))))
