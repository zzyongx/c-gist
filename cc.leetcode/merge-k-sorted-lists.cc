/*
You are given an array of k linked-lists lists, each linked-list is sorted in ascending order.

Merge all the linked-lists into one sorted linked-list and return it.



Example 1:

Input: lists = [[1,4,5],[1,3,4],[2,6]]
Output: [1,1,2,3,4,4,5,6]
Explanation: The linked-lists are:
[
  1->4->5,
  1->3->4,
  2->6
]
merging them into one sorted list:
1->1->2->3->4->4->5->6

Example 2:

Input: lists = []
Output: []

Example 3:

Input: lists = [[]]
Output: []



Constraints:

    k == lists.length
    0 <= k <= 10^4
    0 <= lists[i].length <= 500
    -10^4 <= lists[i][j] <= 10^4
    lists[i] is sorted in ascending order.
    The sum of lists[i].length won't exceed 10^4.

*/

#include "test.h"
#include <cassert>
#include <vector>
#include <queue>
#include <functional>

using namespace std;

class Solution1 {
public:
  ListNode *mergeTwoLists(ListNode *a, ListNode *b) {
    ListNode *head = nullptr;
    ListNode **ptr = &head;
    while (true) {
      if (a && b) {
        if (a->val < b->val) {
          *ptr = a;
          a = a->next;
        } else {
          *ptr = b;
          b = b->next;
        }
        ptr = &((*ptr)->next);
      } else {
        *ptr = a ? a : b;
        break;
      }
    }
    return head;
  };

  // O(k^2 * n)
  ListNode* mergeKLists(vector<ListNode*>& lists) {
    if (lists.empty()) return nullptr;

    ListNode *head = lists[0];
    for (size_t i = 1; i < lists.size(); ++i) {
      head = mergeTwoLists(head, lists[i]);
    }
    return head;
  }
};

class Solution {
public:
  ListNode* mergeKLists(vector<ListNode*> &lists) {
    auto cmp =
      [](const ListNode *a, const ListNode *b) {
        return a->val > b->val;
      };

    priority_queue<ListNode*, vector<ListNode*>, decltype(cmp)> pque(cmp);

    for (auto ite = lists.begin(); ite != lists.end(); ++ite) {
      if (*ite) pque.push(*ite);
    }

    ListNode *head = nullptr;
    ListNode **ptr = &head;

    // O(klog(k)n)
    while (!pque.empty()) {
      ListNode *min = pque.top();
      pque.pop();
      if (min->next) pque.push(min->next);

      *ptr = min;
      ptr = &((*ptr)->next);
    }

    return head;
  }
};


int main() {
  {
    std::shared_ptr<ListNode> head(createListNode({1,3,4}), freeListNode);
    if (head->val != 1 || head->next->val != 3 ||
        head->next->next->val != 4 || head->next->next->next != nullptr) {
      fatal("case1 error");
    }
  }

  Solution solution;

  {
    vector<ListNode*> lists = {
      createListNode({1,4,5}),
      createListNode({1,3,4}),
      createListNode({2,6}),
    };

    auto got = solution.mergeKLists(lists);
    auto want = createListNode({1,1,2,3,4,4,5,6});
    if (!listNodeEqual(want, got)) {
      fatal2("case1", want, got);
    }
  }

  {
    vector<ListNode*> lists;
    auto got = solution.mergeKLists(lists);
    if (got != nullptr) {
      fatal("case2");
    }
  }

  {
    vector<ListNode*> lists = {nullptr};
    auto got = solution.mergeKLists(lists);
    if (got != nullptr) {
      fatal("case3");
    }
  }
}
