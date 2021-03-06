(ns topology.core
  (:require [clojure.string :as str]
            [topology.analyzer :as ta]
            [topology.finder :as tf]))

(defn print-weighted-edges
  [edges]
  (doseq [[[outv inv] w] edges]
    (println (str/join "," [outv inv w]))))

(defn all-ns->fn-edges [& source-paths]
  (let [sources (tf/find-sources source-paths)
        namespaces (set (tf/file-namespaces sources))
        edges (mapcat ta/ns->edges namespaces)]
    (frequencies edges)))
