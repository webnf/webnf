(ns webnf.user-agent
  (:import nl.bitwalker.useragentutils.UserAgent))

(defn parse-user-agent [ua]
  (let [a (UserAgent/parseUserAgentString ua)
        b (.getBrowser a)
        bv (.getBrowserVersion a)
        id (.getId a)
        os (.getOperatingSystem a)]
    {:id id
     :operating-system (when os {:name (.getName os)
                                 :device-type (-> os .getDeviceType .getName)
                                 :group (-> os .getGroup .getName)
                                 :id (-> os .getId)
                                 :manufacturer (-> os .getManufacturer .getName)
                                 :mobile (-> os .isMobileDevice)})
     :browser (when b {:name (.getName b)
                       :version (when bv (.getVersion bv))
                       :type (-> b .getBrowserType .getName)
                       :id (.getId b)
                       :manufacturer (-> b .getManufacturer .getName)
                       :rendering-engine (-> b .getRenderingEngine .name)})}))
