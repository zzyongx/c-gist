#include <iostream>

class Solution {
public:
  int max(int a, int b) {
    return a > b ? a : b;
  }
  
  int max(int a, int b, int c) {
    int ab = a > b ? a : b;
    return ab > c ? ab : c;
  }

  int min(int a, int b, int c) {
    int ab = a > b ? b : a;
    return ab > c ? c : ab;
  }
  
  int maxProduct(int A[], int n) {
    int curMin = A[0];
    int curMax = A[0];
    int allMax = A[0];

    for (int i = 1; i < n; ++i) {
      if (A[i] > 0) {
        curMin = curMin * A[i];
        curMax = max(curMax * A[i], A[i]);
      } else { // <= 0
        int tmin = min(curMin * A[i], A[i], curMax * A[i]);
        curMax = max(curMin * A[i], A[i], curMax * A[i]);
        curMin = tmin;
      }
      
      if (curMax > allMax) allMax = curMax;
    }
    return allMax;
  }
};
int main()
{
  Solution s;
  bool r;

  int a1[] = {1, 2, -1, 3, 4};
  r = (s.maxProduct(a1, sizeof(a1)/sizeof(int)) == 12);
  std::cout << "{1, 2, -1, 3, 4} = 12 " << (r ? "TRUE" : "FALSE") << "\n";

  int a2[] = {1, 2, -1, 3, 4, -2, 2};
  r = (s.maxProduct(a2, sizeof(a2)/sizeof(int)) == 96);
  std::cout << "{1, 2, -1, 3, 4, -2, 2} = 96 " << (r ? "TRUE" : "FALSE") << "\n";

  int a3[] = {-1, -2, -3};
  r = (s.maxProduct(a3, sizeof(a3)/sizeof(int)) == 6);
  std::cout << "{-1, -2, -3} = 6 " << (r ? "TRUE" : "FALSE") << "\n";

  int a4[] = {4, -1, 5, 0, -2, -3};
  r = (s.maxProduct(a4, sizeof(a4)/sizeof(int)) == 6);
  std::cout << "{4, -1, 5, 0, -2, -3} = 6" << (r ? "TRUE" : "FALSE") << "\n";

  int a5[] = {-1, 0, 1};
  r = (s.maxProduct(a5, sizeof(a5)/sizeof(int)) == 1);
  std::cout << "{-1, 0, 1} = 1 " << (r ? "TRUE" : "FALSE") << "\n";

  return 0;
}
