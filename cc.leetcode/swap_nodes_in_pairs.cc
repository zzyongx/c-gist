#include <iostream>

struct ListNode {
    int val;
    ListNode *next;
    ListNode(int x) : val(x), next(NULL) {}
};

class Solution {
public:
  ListNode *swapPairs(ListNode *head) {
    if (head && head->next) {
      ListNode *newHead;
      ListNode **ptr = &newHead;
      while (head && head->next) {
        ListNode *t = head->next->next;
        *ptr = head->next;
        (*ptr)->next = head;
        (*ptr)->next->next = t;
        
        ptr = &((*ptr)->next->next);
        head = t;
      }
      return newHead;
    } else {
      return head;
    }
  }
};

int main()
{
  ListNode n1(1);
  ListNode n2(2);
  ListNode n3(3);
  ListNode n4(4);
  ListNode n5(5);
  ListNode n6(6);
  ListNode n7(7);

  n2.next = &n1;
  n1.next = &n4;
  n4.next = &n3;
  n3.next = &n6;
  n6.next = &n5;
  n5.next = &n7;

  Solution s;
  ListNode *l = s.swapPairs(&n2);

  for (int i = 0; l; i++, l = l->next) {
    if (l->val != i+1) {
      std::cout << "swap pairs FALSE\n";
      return 0;
    }
  }

  std::cout << "swap pairs TRUE\n";
  return 0;
}
