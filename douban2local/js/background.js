// Called when the user clicks on the page action.
chrome.pageAction.onClicked.addListener(function(tab) {
  // No tabs or host permissions needed!
  chrome.pageAction.setTitle({title: tab.url, tabId: tab.id});
});

chrome.tabs.query(
  {currentWindow: true, active : true},
  function(tabs) {
    chrome.pageAction.show(tabs[0].id);
  }
);

chrome.runtime.onInstalled.addListener(function() {
  // Replace all rules ...
  chrome.declarativeContent.onPageChanged.removeRules(undefined, function() {
    // With a new rule ...
    chrome.declarativeContent.onPageChanged.addRules([
      { conditions: [
          new chrome.declarativeContent.PageStateMatcher({
            pageUrl: { hostEquals: "www.douban.com" }
          })
        ],
        // And shows the extension's page action.
        actions: [ new chrome.declarativeContent.ShowPageAction() ]
      }
    ]);
  });
});
