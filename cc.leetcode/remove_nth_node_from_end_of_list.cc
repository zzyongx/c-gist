#include <iostream>

struct ListNode {
  int val;
  ListNode *next;
  ListNode(int x) : val(x), next(NULL) {}
};

class Solution {
public:
  ListNode *removeNthFromEnd(ListNode *head, int n) {
    ListNode *fast = head, **slow = &head;
    while (n--) fast = fast->next;
    while (fast) {
      fast = fast->next;      
      slow = &(*slow)->next;
    }

    *slow = (*slow)->next;
    return head;
  }
};

ListNode *list1()
{
  ListNode *n1 = new ListNode(1);
  ListNode *n2 = new ListNode(2);
  ListNode *n3 = new ListNode(3);
  ListNode *n4 = new ListNode(4);
  ListNode *n5 = new ListNode(5);

  n1->next = n2;
  n2->next = n3;
  n3->next = n4;
  n4->next = n5;

  return n1;
}

int main()
{
  Solution s;
  ListNode *list = list1();

  list = s.removeNthFromEnd(list, 2);
  if (list->val == 1 && list->next->val == 2 &&
      list->next->next->val == 3 &&
      list->next->next->next->val == 5) {
    std::cout << "list{1,2,3,4,5} removeNthEnd 2th TRUE\n";
  } else {
    std::cout << "list{1,2,3,4,5} removeNthEnd 2th FALSE\n";
  }

  list = s.removeNthFromEnd(list, 4);
  if (list->val == 2 && list->next->val == 3 &&
      list->next->next->val == 5) {
    std::cout << "list{1,2,3,5} removeNthEnd 4th TRUE\n";
  } else {
    std::cout << "list{1,2,3,5} removeNthEnd 4th FALSE\n";
  }

  list = s.removeNthFromEnd(list, 1);
  if (list->val == 2 && list->next->val == 3) {
    std::cout << "list{1,2,3} removeNthEnd 1th TRUE\n";
  } else {
    std::cout << "list{1,2,3} removeNthEnd 1th FALSE\n";
  }

  return 0;
}
