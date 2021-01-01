#include <iostream>

class Solution {
public:
  int singleNumberXor(int A[], int n) {
    int one = 0;
    for (int i = 0; i < n; ++i) {
      one ^= A[i];
    }
    return one;
  }

  int singleNumberCom(int A[], int n) {
    int one = 0, two = 0;
    for (int i = 0; i < n; ++i) {
      two = one & A[i];
      one |= A[i];
      one &= ~two;
    }
    return one;
  }
  
  int singleNumber(int A[], int n) {
    return singleNumberXor(A, n);
  }
};

int main()
{
  Solution s;
  int n;

  int A1[] = {1, 2, 4, 1, 4, 3, 2};
  n = s.singleNumberXor(A1, sizeof(A1)/sizeof(int));
  std::cout << "{1, 2, 4, 1, 4, 3, 2} = 3 singleNumberXor " << (n == 3 ? "TRUE" : "FALSE") << "\n";
  n = s.singleNumberCom(A1, sizeof(A1)/sizeof(int));
  std::cout << "{1, 2, 4, 1, 4, 3, 2} = 3 singleNumberCom " << (n == 3 ? "TRUE" : "FALSE") << "\n";

  int A2[] = {1, 2, 4, 3, 2, 1, 4};
  n = s.singleNumberXor(A2, sizeof(A2)/sizeof(int));
  std::cout << "{1, 2, 4, 3, 2, 1, 4} = 3 singleNumberXor " << (n == 3 ? "TRUE" : "FALSE") << "\n";
  n = s.singleNumberCom(A2, sizeof(A2)/sizeof(int));
  std::cout << "{1, 2, 4, 3, 2, 1, 4} = 3 singleNumberCom " << (n == 3 ? "TRUE" : "FALSE") << "\n";

  return 0;
}
