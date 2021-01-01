#include <iostream>

class Solution {
public:
  int removeElement(int A[], int n, int elem) {
    int len = n;
    for (int i = n-1; i >= 0; --i) {
      if (A[i] == elem) {
        --len;        
        if (i != len) {
          int t = A[len];
          A[len] = A[i];
          A[i] = t;
        }
      }
    }
    return len;
  }
};

int main()
{
  Solution s;
  int len;

  int A[] = {1, 2, 3, 2, 4, 2};
  len = s.removeElement(A, sizeof(A)/sizeof(int), 2);
  std::cout << "{1, 2, 3, 2, 4} removeElement = 3 " << (len == 3 ? "TRUE" : "FALSE") << "\n";

  return 0;
}                                                        
