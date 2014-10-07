#include <iostream>

struct ListNode {
  int val;
  ListNode *next;
  ListNode(int x) : val(x), next(NULL) {}
};

class Solution {
public:
  ListNode *deleteDuplicates(ListNode *head) {
    if (!head) return head;
    
    ListNode *ptr = head, *p = head->next;
    while (p) {
      if (p->val == ptr->val) {
        ptr->next = p->next;
      } else {
        ptr = ptr->next;
      }
      p = p->next;
    }
    return head;
  }
};

int main()
{
  ListNode n00(0);
  ListNode n01(0);
  ListNode n10(1);
  ListNode n11(1);
  ListNode n12(1);
  ListNode n20(2);
  ListNode n21(2);
  ListNode n30(3);
  ListNode n31(3);

  n00.next = &n01;
  n01.next = &n10;
  n10.next = &n11;
  n11.next = &n12;
  n12.next = &n20;
  n20.next = &n21;
  n21.next = &n30;
  n30.next = &n31;

  ListNode *head = &n00;

  Solution s;
  head = s.deleteDuplicates(head);
  if (head->val == 0 && head->next->val == 1 &&
      head->next->next->val == 2 && head->next->next->next->val == 3) {
    std::cout << "delete duplicates TRUE";
  } else {
    std::cout << "delete duplicates FALSE";
  }

  return 0;
}
