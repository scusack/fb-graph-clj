# `fb-graph-clj`

A clojure library that provides a simple layer around clj-http to
access the facebook graph api.

## Usage

```clojure
(require '[fb-graph-clj.core :as fb])

(def ac "...see below...")
(fb/with-access-token ac
  (fb/get [:me :friends]))
=> {:status 200,
    :headers
    {"content-type" "text/javascript; charset=UTF-8"
     "date" "Wed, 30 Nov 2011 07:24:32 GMT"
     "cache-control" "private, no-cache, no-store, must-revalidate"
     ...}
    :body
    {:data [...],
     :paging
     {:next     "https://graph.facebook.com:443/me/friends?access_token=...",
      :previous "https://graph.facebook.com:443/me/friends?access_token=..."}}}
```

Facebook often returns a list of objects in the `data` element of the
response body, this is paired with a `paging` element.  It is
convenient to be able to treat the response as sequable.

Here's how you do it:

```clojure
(let [response (fb/get [:me :friends] {:query-params {:limit 10}})]
  (doseq [{:keys [name id] :as friend} (fb/data-seq response)]
    (println "Hello" name "with ID:" id)))
```

`data-seq` is lazy and will only request the next page from facebook
when it needs it.

### How to get an Access Token

In order to make calls against the facebook graph api you need an
Access Token.  There are several ways to do this, you can jump through
several OAUTH hoops and deal with callbacks, redirects, etc.
Alternatively you can go to this [Facebook Developer API
page](http://developers.facebook.com/docs/reference/api/) and copy
one.

The API reference page will handle all of the OAUTH stuff and give you
a set of links with a valid Access Token on the query string.  After
logging into facebook just look down the page for the link to your
friends, cut and paste the access token from the URL into the REPL and
use the `with-access-token` macro and your ready to go.

You can also pass the access token as option if you wish, like this:

```
(fb/get [:me :friends] :access_token "...")
=> [a response containing a list of your friends]
```

## Installation

`fb-graph-clj` is available as a Maven artifact from
[Clojars](http://clojars.org/fb-graph-clj):

```clojure
[fb-graph-clj "1.0.0-SNAPSHOT"]
```

## Related Libraries

If you need a library that has built in handling for OAUTH then check
out
[clj-facebook-graph](https://github.com/maxweber/clj-facebook-graph).

My thanks to `clj-facebook-graph` for providing some inspiration and
the idea behind the [:me :friends] to facebook URL translation.

## License

Released under the MIT License:
<http://www.opensource.org/licenses/mit-license.php>
