#include <iostream>

class Solution {
public:
  /* e .left. max min .right. <e */
  bool search(int A[], int n, int target) {
    int start = 0, end = n;
    while (start < end) {
      int mid = start + (end - start)/2;
      if (target == A[mid]) return true;

      if (A[start] < A[mid]) {  // mid is in left
        if (target > A[mid] || target < A[start]) start = mid+1;
        else end = mid;
      } else if (A[end-1] > A[mid]) { // mid is in right
        if (target < A[mid] || target > A[end-1]) end = mid; 
        else start = mid+1;
      } else {
        return search(A+mid+1, end-(mid+1), target) ||
          search(A+start, mid-start, target);
      }
    }
    return false;
  }
};

int main()
{
  Solution s;
  bool b;

  int A[] = {4,5,6,0,1,2,4,4};
  int N = sizeof(A)/sizeof(int);

  for (int i = 0; i < N; ++i) {
    b = s.search(A, N, A[i]);
    std::cout << "{4,5,6,0,1,2,4,4} search " << A[i] << " true "
              << (b ? "TRUE" : "FALSE") << "\n";
  }

  b = s.search(A, N, -1);
  std::cout << "{4,5,6,0,1,2,3,4} search -1 false " << (!b ? "TRUE" : "FALSE") << "\n";

  b = s.search(A, N, 9);
  std::cout << "{4,5,6,0,1,2,3,4} search 9 false " << (!b ? "TRUE" : "FALSE") << "\n";

  int A1[] = {1, 3, 1, 1};
  int N1 = 4;

  b = s.search(A1, N1, 3);
  std::cout << "{1,2,1,1} search 3 true " << (b ? "TRUE" : "FALSE") << "\n";

  int A2[] = {1,1,1,3,1};
  int N2 = 5;

  b = s.search(A2, N2, 3);
  std::cout << "{1,1,1,3,1} search 3 true " << (b ? "TRUE" : "FALSE") << "\n";

  return 0;
}
