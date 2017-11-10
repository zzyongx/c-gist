var dlUrl = "https://www.douban.com/people/zzyong/statuses";
var stPrefix = "HISTROY_";

function parseQueryString(qs) {
  var query = {};
  qs.split('&').forEach(function(pair) {
    var parts = pair.split('=');
    query[parts[0]] = decodeURIComponent(parts[1]);
  });

  return query;
}

function createHref(url, p, count, init) {
  return url + "?p=" + p + "&countDownLatch=" + count + "&initCountDownLatch=" + init;
}

function createStorageKeys(init) {
  var keys = [];
  for (var i = 0; i < init; ++i) {
    keys.push(stPrefix + i);
  }
  return keys;
}

function currentStorageKey(query) {
  var idx = query.initCountDownLatch - query.countDownLatch - 1;
  return stPrefix + idx;
}

function storageStats(historyInnerHTML) {
  var itemCount = 0;
  var itemSize = 0;
  Object.keys(historyInnerHTML).forEach(function(key) {
    itemCount++;
    itemSize += historyInnerHTML[key].length;
  });
  console.log("ItemSizeLimit: 5242880(5M)");
  console.log("ItemCount:", itemCount, "; ItemSize: ", itemSize);
}

function nextHrefOrPrint(query) {
  var streamItemsDom = window.document.getElementsByClassName("stream-items")[0];
  var innerHTML = streamItemsDom.innerHTML;

  var historyInnerHTML = {};
  var storageKeys = createStorageKeys(query.initCountDownLatch);

  chrome.storage.local.get(storageKeys, function(items) {
    historyInnerHTML[currentStorageKey(query)] = innerHTML;
    Object.keys(items).forEach(function(key) { historyInnerHTML[key] = items[key] });

    storageStats(historyInnerHTML);

    if (query.countDownLatch > 0) {
      chrome.storage.local.set(historyInnerHTML);
      window.setTimeout(function() {
        window.location.href = createHref(dlUrl, parseInt(query.p)+1, query.countDownLatch-1,
                                          query.initCountDownLatch);
      }, 5 * 1000);
    } else {
      var newInnerHTML = ''
      storageKeys.forEach(function(key) {  // order is important
        if (historyInnerHTML[key]) newInnerHTML += historyInnerHTML[key];
      });

      // console.log(Object.keys(historyInnerHTML));
      // console.log(historyInnerHTML);

      streamItemsDom.innerHTML = newInnerHTML;
      alert('打印页面稍后自动打开，届时选择目标打印机为“另存为PDF”即可');

      window.setTimeout(function() {
        window.print();
        window.location.href = dlUrl;
      }, 5 * 1000);
    }
  });
}

var query = parseQueryString(window.location.search.slice(1));
console.log(query);
if (query.countDownLatch !== undefined) nextHrefOrPrint(query);

chrome.runtime.onMessage.addListener(
  function(request, sender, sendResponse) {
    window.location.href = createHref(dlUrl, request.start, request.count-1, request.count);
  });
