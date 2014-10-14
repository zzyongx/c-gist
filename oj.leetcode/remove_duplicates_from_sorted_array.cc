#include <iostream>

class Solution {
public:
  int removeDuplicates(int A[], int n) {
    if (n == 0) return 0;
    
    int len = 0;
    for (int i = 1; i < n; ++i) {
      if (A[i] != A[len]) {
        A[++len] = A[i];
      }
    }
    return len+1; 
  }
};

int main()
{
  Solution s;
  int len;

  int A1[] = {2};
  len = s.removeDuplicates(A1, 1);
  std::cout << "{2} remove duplicates = 1 " << (len == 1 ? "TRUE" : "FALSE") << "\n";

  int A2[] = {1, 2, 2, 3, 4, 5, 5};
  len = s.removeDuplicates(A2, sizeof(A2)/sizeof(int));
  std::cout << "{1, 2, 2, 3, 5, 5, 4} remove duplicates = 5 " << (len == 5 ? "TRUE" : "FALSE") << "\n";

  return 0;  
}
