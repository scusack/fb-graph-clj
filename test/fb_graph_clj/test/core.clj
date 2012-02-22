(ns fb-graph-clj.test.core
  (:use [clojure.test]
        [clojure.pprint :only [pprint]])
  (:require [fb-graph-clj.core :as fb])
  (:import [java.net URL]))

(defn get-access-token
  "Set the property in the repl using:
   (System/setProperty `fb-graph-clj.access-token` `get-one from http://developers.facebook.com/docs/reference/api/`)"
  []
  (System/getProperty "fb-graph-clj.access-token"))

(deftest test-make-req
  (are [url method options req] (= (fb/make-req url method options)
                                   req)
       "http://test/" :post nil
       {:url "http://test/" :method :post}

       "http://test/" :get {:something 123}
       {:url "http://test/"
        :method    :get
        :something 123}

       [:me :friends] :get nil
       {:url "https://graph.facebook.com/me/friends" :method :get})

  (fb/with-access-token "ACCESS-TOKEN"
    (is (= (fb/make-req "http://with-access-token/" :get {:something 123})
           {:url          "http://with-access-token/"
            :method       :get
            :something    123
            :query-params {:access_token "ACCESS-TOKEN"}}))

    (fb/without-access-token
     (is (= (fb/make-req "http://without-access-token/" :get {:something 123})
            {:url          "http://without-access-token/"
             :method       :get
             :something    123})))))

(deftest test-data-seq
  ;; You need to have at least 3 friends for this test to work.

  (fb/with-access-token (get-access-token)
    ;; start by getting our first friend
    (let [r1 (fb/get [:me :friends] {:query-params {:limit 1}})
          {:keys [data paging] :as response} (:body r1)]
      (is (= (count data) 1))

      (is (= (keys (first data))
             [:name :id]))

      ;; then explicitly get the 'next' url, which having carried over
      ;; the previous limit should have only one friend in it
      (let [r2 (fb/without-access-token
                (fb/get (:next paging)))
            {:keys [data paging] :as response} (:body r2)]
        (is (= (count data) 1)))

      ;; now data-seq on the original response and it should do the
      ;; same thing but it will take care of the concat'ing for us
      (is (= 2 (count (take 2 (fb/data-seq r1)))))

      (is (= 3 (count (take 3 (fb/data-seq r1))))))))

(defn do-get
  [url]
  (fb/with-access-token (get-access-token)
    (fb/get url)))
