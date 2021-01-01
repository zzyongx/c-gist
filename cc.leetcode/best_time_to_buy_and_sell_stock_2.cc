#include <iostream>
#include <vector>

class Solution {
public:
  /* increment subarray */
  int maxProfit(std::vector<int> &prices) {
    size_t profit = 0;
    size_t begin = 0;
    for (size_t i = 1; i < prices.size(); ++i) {
      if (prices[i] < prices[i-1]) {
        profit += (prices[i-1] - prices[begin]);
        begin = i;
      }
    }
    if (begin < prices.size()) {
      profit += prices.back() - prices[begin];
    }
    return profit;
  }
};

int main()
{
  Solution s;
  int n;

  int A1[] = {1, 2, 3, 5, 4, 1, 10, 9, 12};
  std::vector<int> V1(A1, A1 + sizeof(A1)/sizeof(int));
  n = s.maxProfit(V1);
  std::cout << "{1, 2, 3, 5, 4, 1, 10, 9, 12} maxProfit " << (n == 16 ? "TRUE" : "FALSE") << "\n";

  int A2[] = {5, 4, 3, 2};
  std::vector<int> V2(A2, A2 + sizeof(A2)/sizeof(int));
  n = s.maxProfit(V2);
  std::cout << "{5, 4, 3, 2} maxProfit " << (n == 0 ? "TRUE" : "FALSE") << "\n";

  int A3[] = {2, 3, 4, 5};
  std::vector<int> V3(A3, A3 + sizeof(A3)/sizeof(int));
  n = s.maxProfit(V3);
  std::cout << "{2, 3, 4, 5} maxProfit " << (n == 3 ? "TRUE" : "FALSE") << "\n";

  return 0;  
}
  
