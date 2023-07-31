(ns flybot.client.web.core.dom.profile
  (:require [clojure.string :as str]
            [flybot.client.web.core.dom.common.link :as link]
            [flybot.client.web.core.dom.common.svg :as svg]
            [flybot.client.web.core.utils :as web.utils]
            [re-frame.core :as rf]))

(defn format-date
  [date]
  (-> (js/Intl.DateTimeFormat. "en-GB")
      (.format date)))

(defn post-link
  "For the blog page, returns a link to the individual post,
   for the other pages, returns a link to the page itself."
  [{:post/keys [page id] :as post} text]
  (if (= :blog page)
    (link/internal-link :flybot/blog-post
                        text
                        true
                        {:id-ending (link/truncate-uuid id)
                         :url-identifier (web.utils/post->url-identifier post)})
    (link/internal-link (->> page name (str "flybot/") keyword)
                        text
                        true)))

(defn user-info
  [username date1 date2 page]
  [:div.post-author
   [:div {:key "pen-icon"} svg/pen-icon]
   [:div {:key "user-name"} (str username)]
   [:div {:key "clock-icon"} svg/clock-icon]
   [:div {:key "date1"} (format-date date1)]
   [:div {:key "action"} "(Created)"]
   (when date2
     [:<>
      [:div {:key "date2"} (format-date date2)]
      [:div {:key "action"} "(Edited)"]])
   [:div {:key "page-icon"} svg/page-icon]
   [:div {:key "page"} (-> page name str/upper-case)]])

(defn post-short
  [{:post/keys [id page css-class creation-date last-edit-date] :as post}]
  (let [post-title (web.utils/post->title post)]
    [:div.post.short
     {:key id
      :id id}
     [post-link post
      [:div.post-body
       {:class css-class}
       [:h3 post-title]
       [:div.post-authors
        (user-info "You" creation-date last-edit-date page)]]]]))

(defn profile-page
  []
  [:section.container.profile
   (let [{:user/keys [id email roles] user-name :user/name}
         @(rf/subscribe [:subs/pattern '{:app/user ?x}])
         all-posts      (vals @(rf/subscribe [:subs/pattern {:app/posts '?x}]))
         posts-created  (filter #(= id (-> % :post/author :user/id)) all-posts)
         posts-edited   (->> all-posts
                             (filter :post/last-editor)
                             (filter #(= id (-> % :post/last-editor :user/id))))]
     (if email
       [:<>
        [:h1 "User Profile: " user-name]
        [:div.perso-details
         [:div
          [:h2 "Your Personal Details"]
          [:p (str "Email: ") [:strong email]]
          [:p (str "Total Posts Created: ") [:strong (count posts-created)]]
          [:p (str "Total Posts Edited: ") [:strong (count posts-edited)]]]
         [:div
          [:h2 "Your Roles"]
          (doall
           (for [role roles
                 :let [{role-name :role/name date :role/date-granted} role]]
             [:p {:key role-name}
              [:strong (-> role-name name str/upper-case)] (str " role granted on " (format-date date))]))]]
        [:div.blog
         [:h2 "Your Posts"]
         (doall
          (for [post posts-created]
            (post-short post)))]]
       [:div
        [:h2 "You are not logged in."]
        [:p "This section is dedicated to logged-in users."]]))])