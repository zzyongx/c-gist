#include <iostream>
#include <vector>

class Solution {
public:
  /* the first row target < */
  int searchRow(std::vector< std::vector<int> > &matrix, int target) {
    size_t begin = 0, end = matrix.size();
    while (begin < end) {
      size_t mid = begin + (end - begin)/2;
      if (target < matrix[mid][0]) {
        end = mid;
      } else {
        begin = mid + 1;
      }
    }

    return end-1;
  }

  bool searchMatrix(std::vector<std::vector<int> > &matrix, int target) {
    int row = searchRow(matrix, target);
    if (row < 0) return false;

    size_t begin = 0, end = matrix[row].size();
    while (begin < end) {
      size_t mid = begin + (end - begin)/2;
      if (target == matrix[row][mid]) return true;
      else if (target < matrix[row][mid]) end = mid;
      else begin = mid+1;
    }
    return false;    
  }
};

int main()
{
  bool b;
  Solution s;
  
  std::vector< std::vector<int> > matrix;

  int A1[] = {1,   3,  5,  7};
  matrix.push_back(std::vector<int>(A1, A1+4));

  int A2[] = {10, 11, 16, 20};
  matrix.push_back(std::vector<int>(A2, A2+4));

  int A3[] = {23, 30, 34, 50};
  matrix.push_back(std::vector<int>(A3, A3+4));

  b = s.searchMatrix(matrix, 16);
  std::cout << "matrix search 16 = true " << (b ? "TRUE" : "FALSE") << "\n";

  b = s.searchMatrix(matrix, 0);
  std::cout << "matrix search 0 = false " << (!b ? "TRUE" : "FALSE") << "\n";

  b = s.searchMatrix(matrix, 100);
  std::cout << "matrix search 100 = false " << (!b ? "TRUE" : "FALSE") << "\n";
  
  b = s.searchMatrix(matrix, 45);
  std::cout << "matrix search 45 = false " << (!b ? "TRUE" : "FALSE") << "\n";

  return 0;  
}
