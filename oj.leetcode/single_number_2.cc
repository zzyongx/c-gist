#include <iostream>

class Solution {

public:

  int singleNumber(int A[], int n) {
    int one = 0, two = 0, three = 0;
    for (int i = 0; i < n; ++i) {
      three = two & A[i];
      two = two | (one & A[i]);
      one = one | A[i];

      one &= ~three;
      two &= ~three;
    }
    return one;
  }
};

int main()
{
  Solution s;
  int n;
  
  int A1[] = {1, 1, 2, 1};
  n = s.singleNumber(A1, 4);
  std::cout << "{1, 1, 2, 1} = 2 signleNumber " << (n == 2 ? "TRUE" : "FALSE") << "\n";

  int A2[] = {3, 1, 1, 3, 2, 3, 1};
  n = s.singleNumber(A2, sizeof(A2)/sizeof(int));
  std::cout << "{3, 1, 1, 3, 2, 3, 1} = 2 singleNumber " << (n == 2 ? "TRUE" : "FALSE") << "\n";
}
