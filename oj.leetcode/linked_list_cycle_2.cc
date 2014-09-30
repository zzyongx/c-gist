#include <stdint.h>
#include <iostream>

struct ListNode {
  int val;
  ListNode *next;
  ListNode(int x) : val(x), next(NULL) {}
};

void printList(ListNode *head, int n, const char *tips = 0)
{
  if (tips) std::cout << tips << "\n";
  ListNode *ptr = head;
  for (int i = 0; i < n; ++i) {
    std::cout << ptr << " " << ptr->val << " " << ptr->next << "\n";
    ptr = ptr->next;
  }
}

class Solution {
public:
  /* 1: use the lower bit in point as flag */
  ListNode *ptrMarkMethod(ListNode *head) {
    int i = 0;
    ListNode *node = NULL;
    ListNode *p = head;
    while (p) {
      if ((uintptr_t) p->next & 0x01) {
        node = p;
        break;
      }
      i++;
      ListNode *next = p->next;
      p->next = (ListNode *)((uintptr_t) p->next | 0x01);
      p = next;
    }
    
    p = head;
    while (i > 0) {
      p->next = (ListNode *)((uintptr_t) p->next & (~0 << 1));
      p = p->next;
      i--;
    }

    return node;
  }

  /* 2: reverse the list, if back to head, cycle exists
   *    but we don't know where the cycle happens
   */
  bool reverseListMethod(ListNode *head) {
    bool hasCycle = false;
    ListNode *thead = head;
    for (size_t i = 0; i < 2; ++i) {
      ListNode *prev = NULL, *p = thead;
      while (p) {
        thead = p;
        ListNode *next = p->next;
        p->next = prev;
        if (p == head) hasCycle = true;
        prev = p;
        p = next;
      }
    }
    return hasCycle;
  }

  /* 3: fast-slow ptr */
  ListNode *fastSlowPtrMethod(ListNode *head) {
    /* one ptr move fast, one move slow,
     * if they meet again (first meet point is head), cycyle exists
     */
    ListNode *fast = head, *slow = head;
    while (fast && fast->next) {
      slow = slow->next;
      fast = fast->next->next;
      if (slow == fast) break; /* meet again */
    }

    if (!fast || !fast->next) return NULL;

    /* (x + y)/(x + y + nr) = 1/2
     * x + y = nr; x = nr - y
     */
    slow = head;
    while (slow != fast) {
      slow = slow->next;
      fast = fast->next;
    }
    return slow;
  }

  ListNode *detectCycle(ListNode *head) {
    // printList(head, 4, "init value");

    ListNode *m1 = ptrMarkMethod(head);
    // printList(head, 4, "after ptrMarkMethod");
    
    bool found = reverseListMethod(head);
    // printList(head, 4, "after reverseListMethod");

    ListNode *m2 = fastSlowPtrMethod(head);
    // printList(head, 4, "after fastSlowPtrMethod");

    if (m1 == m2 && (m1 != NULL) == found) {
      return m1;
    } else {
      return NULL;
    }
  }
};

int main()
{
  ListNode l1(1);
  ListNode l2(2);
  ListNode l3(3);
  ListNode l4(4);

  l1.next = &l2;
  l2.next = &l3;
  l3.next = &l4;

  // printList(&l1, 4);

  Solution s;
  ListNode *ptr;
  
  ptr = s.detectCycle(&l1);
  if (ptr) {
    std::cout << "there is cycle " << ptr->val << "\n";
  } else {
    std::cout << "there is no cycle\n";
  }
  
  l4.next = &l2;
  
  // printList(&l1, 5);
  
  ptr = s.detectCycle(&l1);
  if (ptr) {
    std::cout << "there is cycle " << ptr->val << "\n";
  } else {
    std::cout << "there is no cycle\n";
  }

  return 0;
}
