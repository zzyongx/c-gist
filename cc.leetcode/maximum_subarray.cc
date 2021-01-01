#include <iostream>

class Solution {
public:
  int max(int a, int b) {
    return a > b ? a : b;
  }
  
  int maxSubArray(int A[], int n) {
    int maxAll = A[0];
    int maxCur = A[0];

    for (int i = 1; i < n; i++) {
      maxCur = max(A[i], A[i] + maxCur);
      maxAll = max(maxAll, maxCur);
    }
    return maxAll;
  }
};

int main()
{
  Solution s;
  int n;

  int A[] = {-2, 1, -3, 4, -1, 2, 1, -5, 4};
  n = s.maxSubArray(A, sizeof(A)/sizeof(int));
  std::cout << "{-2, 1, -3, 4, -1, 2, 1, -5, 4} = 6 " << (n == 6 ? "TRUE" : "FALSE") << "\n";

  return 0;
}
