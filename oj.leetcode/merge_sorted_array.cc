#include <iostream>

class Solution {
public:
  void merge(int A[], int m, int B[], int n) {
    int last = m+n-1;
    int ai, bi;
    for (ai = m-1, bi = n-1; ai >= 0 && bi >= 0; /* */) {
      if (B[bi] >= A[ai]) {
        A[last--] = B[bi];
        bi--;
      } else {
        A[last--] = A[ai];
        ai--;
      }
    }
    for (int i = bi; i >= 0; i--) {
      A[last--] = B[i];
    }
  }
};

int main()
{
  Solution s;
  int i;

  int A11[] = {1, 2, 3, 4, 0, 0, 0};
  int A12[] = {5, 6, 7};
  s.merge(A11, 4, A12, 3);
  for (i = 0; i < 7; ++i) {
    if (A11[i] != i+1) {
      std::cout << "merge {1,2,3,4} {5,6,7} FALSE\n";
      break;
    }
  }
  if (i == 7) {
    std::cout << "merge {1,2,3,4} {5,6,7} TRUE\n";
  }

  int A21[] = {2, 3, 5, 7, 0, 0, 0};
  int A22[] = {1, 4, 6};
  s.merge(A21, 4, A22, 3);
  for (i = 0; i < 7; ++i) {
    if (A21[i] != i+1) {
      std::cout << "merge {2,3,5,7} {1,4,6} FALSE\n";
      break;
    }
  }
  if (i == 7) {
    std::cout << "merge {1,2,3,4} {5,6,7} TRUE\n";
  }
  
  return 0;
}
