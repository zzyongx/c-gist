#include <iostream>

struct ListNode {
  int val;
  ListNode *next;
  ListNode(int x) : val(x), next(NULL) {}
};

class Solution {
public:
  ListNode *mergeTwoLists(ListNode *l1, ListNode *l2) {
    ListNode *head;
    ListNode **ptr = &head;
    
    if (l1 && l2) {
      while (l1 && l2) {
        if (l1->val < l2->val) {
          *ptr = l1;
          l1 = l1->next;
        } else {
          *ptr = l2;
          l2 = l2->next;
        }
        ptr = &((*ptr)->next);
      }
    }
      
    if (l1) *ptr = l1;
    else *ptr = l2;
    return head;
  }
};

ListNode *list1()
{
  ListNode *n1 = new ListNode(1);
  ListNode *n2 = new ListNode(2);
  ListNode *n3 = new ListNode(3);
  ListNode *n5 = new ListNode(5);

  n1->next = n2;
  n2->next = n3;
  n3->next = n5;

  return n1;
}

ListNode *list2()
{
  ListNode *n2 = new ListNode(2);
  ListNode *n3 = new ListNode(3);
  ListNode *n4 = new ListNode(4);

  n2->next = n3;
  n3->next = n4;

  return n2;
}

int main()
{
  Solution s;

  ListNode *l1 = list1();
  ListNode *l2 = list2();

  ListNode *l = s.mergeTwoLists(l1, l2);
  int A[] = {1, 2, 2, 3, 3, 4, 5};
  for (size_t i = 0; i < sizeof(A)/sizeof(int); i++) {
    if (l->val != A[i]) {
      std::cout << "merge sorted lists FALSE\n";
      return 0;
    }
    l = l->next;
  }
  std::cout << "merge sorted lists TRUE\n";
  return 0;
}  
