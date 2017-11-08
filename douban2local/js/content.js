var max = -1

var query = {};
window.location.search.slice(1).split('&').forEach(function(pair) {
  var parts = pair.split('=');
  query[parts[0]] = decodeURIComponent(parts[1]);
});

var href = window.location.href;
var nextHref;
if (query.p === undefined) {
  nextHref = href + (window.location.search == "" ? "?p=2" : "&p=2");
  query.p = 2;
} else {
  query.p++;
  nextHref = href.replace(/p=\d+/, "p=" + query.p);
}

// chrome.runtime.sendMessage({greeting: nextHref}, function(response) {
//   console.log(response);
// });

// if first page cleanup
if (query.p == 2) chrome.storage.local.clear();

var streamItemsDom = window.document.getElementsByClassName("stream-items")[0];
var innerHTML = streamItemsDom.innerHTML;

var keys = [];
for (var i = 1; i < max; ++i) {
  keys.push("history_" + i);
}

var historyInnerHTML = {};
chrome.storage.local.get(keys, function(items) {
  historyInnerHTML["history_" + (query.p-1)] = innerHTML;
  Object.keys(items).forEach(function(key) { historyInnerHTML[key] = items[key] });

  if (query.p < max) {
    chrome.storage.local.set(historyInnerHTML);
    window.setTimeout(function() {
      console.log(historyInnerHTML);
      window.location.href = nextHref;
    }, 5 * 1000);
  } else {
    /*
    var newInnerHTML = ''
    keys.forEach(function(key) {
      if (historyInnerHTML[key]) newInnerHTML += historyInnerHTML[key];
    });

    streamItemsDom.innerHTML = newInnerHTML;
    alert('打印页面稍后自动打开，届时选择目标打印机为“另存为PDF”即可');
    window.setTimeout(function() {
      window.print();
    }, 5 * 1000); */
  }
});

chrome.runtime.onMessage.addListener(
  function(request, sender, sendResponse) {
    alert(JSON.stringify(request));
  });
