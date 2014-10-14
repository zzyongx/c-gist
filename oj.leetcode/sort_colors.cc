#include <iostream>

class Solution {
public:
  /* two-pass counting sort
   * one-pass */
  void sortColors(int A[], int n) {
    int first1 = -1;
    for (int i = 0, j = n-1; j >= i; /* */) {
      if (A[i] == 0) {
        if (first1 != -1) {
          A[first1] = 0;
          A[i] = 1;
          first1++;
        }
        i++;
      } else if (A[i] == 1) {
        if (first1 == -1) first1 = i;
        i++;
      } else { /* A[i] == 2 */
        if (A[j] != 2) {
          A[i] = A[j];
          A[j] = 2;
        }
        j--;
      }
    }
  }
};

int main()
{
  Solution s;

  int A1[] = {2, 0, 2, 0, 1, 0, 1, 0, 1};
  s.sortColors(A1, 9);
  for (int i = 0; i < 9; ++i) {
    std::cout << A1[i] << " ";
  }
  std::cout << "\n";
  if (A1[0] == 0 && A1[1] == 0 && A1[2] == 0 && A1[3] == 0 &&
      A1[4] == 1 && A1[5] == 1 && A1[6] == 1 &&
      A1[7] == 2 && A1[8] == 2) {
    std::cout << "sort color {2, 0, 2, 0, 1, 0, 1, 0, 1} TRUE\n";
  } else {
    std::cout << "sort color {2, 0, 2, 0, 1, 0, 1, 0, 1} FALSE\n";
  }

  int A2[] = {1, 2, 0};
  s.sortColors(A2, 3);
  for (int i = 0; i < 3; ++i) {
    std::cout << A2[i] << " ";
  }
  std::cout << "\n";
  if (A2[0] == 0 && A2[1] == 1 && A2[2] == 2) {
    std::cout << "sort color {1, 2, 0} TRUE\n";
  } else {
    std::cout << "sort color {1, 2, 0} FALSE\n";
  }

  int A3[] = {1,2,2,2,2,0,0,0,1,1};
  s.sortColors(A3, 10);
  for (int i = 0; i < 10; ++i) {
    std::cout << A3[i] << " ";
  }
  std::cout << "\n";
  if (A3[0] == 0 && A3[1] == 0 && A3[2] == 0 &&
      A3[3] == 1 && A3[4] == 1 && A3[5] == 1 &&
      A3[6] == 2 && A3[7] == 2 && A3[8] == 2 && A3[9] == 2) {
    std::cout << "sort color {1,2,2,2,2,0,0,0,1,1} TRUE\n";
  } else {
    std::cout << "sort color {1,2,2,2,2,0,0,0,1,1} FALSE\n";
  }

  return 0;
}
