(ns bee-record.sql-test
  (:require [bee-record.sql :as sql]
            [clojure.string :as str]
            [midje.sweet :refer :all]))

(reset! sql/quoting :mysql)
(fact "will generate a SQL given a honey struct"
  (sql/to-sql {:select [:a]}) => ["SELECT `a`"])

(def users
  (sql/model {:table "users"
              :pk :id
              :fields [:id :first-name]}))

(defn as-sql [model]
  (update (sql/to-sql model) 0 str/trim))

(fact "will generate a default query given a model"
  (as-sql users)
  => [(str "SELECT `users`.`id` AS `users/id`, "
           "`users`.`first_name` AS `users/first-name` "
           "FROM `users`")])

(facts "when generating single-table queries"
  (fact "will override SELECT"
    (-> users (sql/select [:name :age]) as-sql)
    => [(str "SELECT `users`.`name` AS `users/name`, "
             "`users`.`age` AS `users/age` "
             "FROM `users`")])

  (fact "will add fields to SELECT"
    (-> users (sql/select+ [:name]) as-sql)
    => [(str "SELECT `users`.`id` AS `users/id`, "
             "`users`.`first_name` AS `users/first-name`, "
             "`users`.`name` AS `users/name` "
             "FROM `users`")])

  (fact "will add WHERE clauses"
    (-> users (sql/where '(:= :id 1)) as-sql)
    => (just [#"WHERE `users`\.`id` = ?" 1])))

(facts "about WHERE clauses"
  (tabular
   (fact "normalizes WHERE clauses"
     (-> users (sql/where ?where) as-sql first) => ?result)
   ?where                               ?result
   [:= :name :foo]                      #"`users`.`name` = `users`.`foo`"
   [:and [:= :name "boo"] [:= :id 10]]  #"`users`.`name` = \? AND `users`.`id` = \?"
   '(:= :name :foo)                     #"`users`.`name` = `users`.`foo`"
   {:name 10}                           #"`users`.`name` = ?"))
