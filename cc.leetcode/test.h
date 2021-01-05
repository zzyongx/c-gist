#ifndef _TEST_H_
#define _TEST_H_

#include <vector>
#include <iostream>
#include <algorithm>
#include <iterator>
#include <memory>

struct ListNode {
  int val;
  ListNode *next;
  ListNode() : val(0), next(nullptr) {}
  ListNode(int x) : val(x), next(nullptr) {}
  ListNode(int x, ListNode *next) : val(x), next(next) {}
};

inline void
printListNode(const ListNode *head, const char *sep = ",") {
  while (head) {
    std::cerr << head->val << sep;
    head = head->next;
  }
}

inline bool listNodeEqual(const ListNode *a, const ListNode *b) {
  while (a && b) {
    if (a->val != b->val) return false;
    a = a->next;
    b = b->next;
  }
  return a == nullptr && b == nullptr;
}

inline ListNode *createListNode(std::initializer_list<int> init) {
  ListNode *head = nullptr;
  ListNode **ptr = &head;
  for (auto ite = init.begin(); ite != init.end(); ++ite) {
    *ptr = new ListNode(*ite);
    ptr = &((*ptr)->next);
  }
  return head;
}

inline void freeListNode(ListNode *head) {
  while (head) {
    auto next = head->next;
    delete head;
    head = next;
  }
}

template <class T>
T& vsort(T &v) {
  std::sort(v.begin(), v.end());
  return v;
}

template <class T>
void echo(const char *test, const std::vector<T> &v, const char *sep = " ") {
  std::cout << test << ": ";
  std::copy(v.begin(), v.end(), std::ostream_iterator<T>(std::cout, sep));
  std::cout << "\n";
}

template <class T>
void fatal(const char *test, const T want, const T got) {
  std::cerr << test <<  " ERROR: want " << want
            << ", got " << got << std::endl;
  exit(1);
}

template <class T>
void fatal(const char *test, const std::vector<T> &want, const std::vector<T> &got, const char *sep = ",") {
  std::cerr << test << " ERROR: want\n";
  std::copy(want.begin(), want.end(), std::ostream_iterator<T>(std::cerr, sep));
  std::cerr << "\ngot\n";
  std::copy(got.begin(), got.end(), std::ostream_iterator<T>(std::cerr, sep));
  std::cerr << std::endl;
  exit(1);
}

void fatal(const char *test, const bool want, const bool got) {
  std::cerr << test <<  " ERROR: want " << (want ? "true" : "false")
            << ", got " << (got ? "true" : "false") << std::endl;
  exit(1);
}

void fatal2(const char *test, const ListNode *want, const ListNode *got) {
  std::cerr << test << " ERROR: want\n";
  printListNode(want);
  std::cerr << "\ngot\n";
  printListNode(got);
  std::cerr << std::endl;
  exit(1);
}

inline void fatal(const char *test) {
  std::cerr << test << std::endl;
  exit(1);
}

#endif
