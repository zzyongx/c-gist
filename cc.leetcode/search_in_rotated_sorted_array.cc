#include <iostream>

class Solution {
public:
  /* e .left. max min .right. <e
   * if A[mid] == A[start] == A[end-1]
   * when can't figure out mid's location
   */
  int search(int A[], int n, int target) {
    int start = 0, end = n;
    while (start < end) {
      int mid = start + (end - start)/2;
      // std::cout << start << " " << end << " " << A[mid] << "\n";
      if (target == A[mid]) {
        return mid;
      } else if (A[start] < A[mid]) {  // mid is in left
        if (target > A[mid] || target < A[start]) start = mid+1;
        else end = mid;
      } else { // mid is in right
        if (target < A[mid] || target > A[end-1]) end = mid; 
        else start = mid+1;
      }
    }
    return -1;
  }
};

int main()
{
  Solution s;
  int n;

  int A[] = {4,5,6,7,0,1,2};
  int N = sizeof(A)/sizeof(int);

  n = s.search(A, N, 6);
  std::cout << "{4,5,6,7,0,1,2} search 6 = 2 " << (n == 2 ? "TRUE" : "FALSE") << "\n";

  n = s.search(A, N, 2);
  std::cout << "{4,5,6,7,0,1,2} search 2 = 6 " << (n == 6 ? "TRUE" : "FALSE") << "\n";

  n = s.search(A, N, -1);
  std::cout << "{4,5,6,7,0,1,2} search -1 = -1 " << (n == -1 ? "TRUE" : "FALSE") << "\n";

  n = s.search(A, N, 9);
  std::cout << "{4,5,6,7,0,1,2} search 9 = -1 " << (n == -1 ? "TRUE" : "FALSE") << "\n";

  int A1[] = {7,8,1,2,3,4,5,6};
  int N1 = sizeof(A1)/sizeof(int);

  n = s.search(A1, N1, 2);
  std::cout << "{7,8,1,2,3,4,5,6} search 2 = 3 " << (n == 3 ? "TRUE" : "FALSE") << "\n";

  int A2[] = {3,1};
  int N2 = sizeof(A2)/sizeof(int);

  n = s.search(A2, N2, 3);
  std::cout << "{3,1} search 3 = 0 " << (n == 0 ? "TRUE" : "FALSE") << "\n";

  return 0;
}
