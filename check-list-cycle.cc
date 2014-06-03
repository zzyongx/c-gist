#include <stdint.h>
#include <iostream>

struct ListNode {
    int val;
    ListNode *next;
    ListNode(int x) : val(x), next(NULL) {}
};

class Solution {
public:
    bool hasCycle(ListNode *head) {
        if (head == NULL)
            return false;

        ListNode* p = head;
        while (p->next) {
            if ((uintptr_t) p->next & 0x01) {
                return true;
            }
            ListNode *t = p->next;
            p->next = (ListNode *)((uintptr_t) p->next | 0x01);
            p = t;
        }

        return false;
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
    l4.next = &l2;

    Solution s;
    std::cout <<  "there is cycle " << s.hasCycle(&l1) << std::endl;

    return 0;
}
