(ns topology.clique
  (:require
   [clojure.zip :as zip]
   [clojure.repl :as repl]))

;; h/t lein-clique 0.1.2
;; https://github.com/Hendekagon/lein-clique/blob/a71845a69f8c0ce9724b217e82ae8ce47012fa39/src/clique/core.clj

(defn functions
  "Returns all the functions in the namespace ns"
  [ns]
  (try
    ((fn []
       (require ns)
       (map meta (vals (ns-interns ns)))))
    (catch Exception e
      (println e))))

(defn fqns
  "Returns the fully qualified namespace of the given symbol s in namespace ns"
  ([ns s]
   (if-let [rns (-> (ns-resolve ns s) meta :ns)]
     (symbol (str rns) (name s))
     s)))

(defn seq-map-zip [x]
  (zip/zipper
   (fn [n] (or (seq? n) (map? n) (vector? n)))
   (fn [b] (if (map? b) (seq b) b))
   (fn [node children] (with-meta children (meta node)))
   x))

(defn zip-nodes [x]
  (take-while (complement zip/end?) (iterate zip/next (seq-map-zip x))))

(defn symbols [x]
  (filter symbol? (map zip/node (zip-nodes x)))) ; also returns Java classes

(defn namespaced-symbols [expression]
  (filter namespace (symbols expression)))

(defn sources [fxns nspc]
  (filter (comp identity second)
          (map vector
               (map :name fxns)
               (map (comp repl/source-fn symbol (partial str nspc \/) :name) fxns))))

(defn dependencies
  "returns all functions used by each function in the given namespace"
  ([namespace]
   (dependencies namespace (functions namespace)))
  ([namespace functions]
   (into
    {}
    (filter (comp not-empty second)
            (map
             (fn [[fn-name source]]
               [(symbol (str namespace) (str fn-name)) (symbols (read-string source))])
             (sources functions namespace))))))

(defn except
  "Filter out symbols not in exclude"
  [sc exclude]
  (filter #(not (= % exclude)) sc))

(defn filtered
  "Filter out symbols in exclude"
  [ds]
  (reduce
   (fn [r [f sc]]
     (assoc r f
            (except (map (partial fqns (symbol (namespace f))) (filter (comp identity namespace) sc)) f)))
   {}
   ds))

(defn all-fq
  "Filter out symbols in exclude"
  [ds]
  (reduce
   (fn [r [f sc]]
     (assoc r f (map (partial fqns (symbol (namespace f))) sc)))
   {}
   ds))

;; (pprint (filtered (all-fq (dependencies 'topology.analyzer))))
