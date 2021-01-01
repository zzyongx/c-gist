#include <iostream>

class Solution {
public:
  int searchInsert(int A[], int n, int target) {
    for (int i = 0; i < n; ++i) {
      if (A[i] >= target) return i;
    }
    return n;
  }
};

int main()
{
  Solution s;
  int n;

  int A[] = {1, 3, 5, 6};
  
  n = s.searchInsert(A, 4, 5);
  std::cout << "{1, 3, 5, 6} searchInsert 5 " << (n == 2 ? "TRUE" : "FALSE") << "\n";

  n = s.searchInsert(A, 4, 2);
  std::cout << "{1, 3, 5, 6} searchInsert 2 " << (n == 1 ? "TRUE" : "FALSE") << "\n";
  
  n = s.searchInsert(A, 4, 7);
  std::cout << "{1, 3, 5, 6} searchInsert 7 " << (n == 4 ? "TRUE" : "FALSE") << "\n";
  
  n = s.searchInsert(A, 4, 0);
  std::cout << "{1, 3, 5, 6} searchInsert 0 " << (n == 0 ? "TRUE" : "FALSE") << "\n";

  return 0;
}
