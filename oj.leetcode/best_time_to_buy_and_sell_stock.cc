#include <iostream>
#include <vector>

class Solution {
public:
  int maxProfit(std::vector<int> &prices) {
    if (prices.empty()) return 0;
    
    int min = prices[0];
    int profit = 0;
    
    for (size_t i = 1; i < prices.size(); ++i) {
      if (prices[i] - min > profit) {
        profit = prices[i] - min;
      }
      if (prices[i] < min) {
        min = prices[i];
      }
    }

    return profit;
  }
};

int main(int argc, char *argv[])
{
  Solution s;
  int n;

  int A1[] = {3, 2, 5, 1, 2, 5};
  std::vector<int> v1(A1, A1+sizeof(A1)/sizeof(int));
  n = s.maxProfit(v1);
  std::cout << "{3, 2, 5, 1, 2, 5} maxProfit = 4 " << (n == 4 ? "TRUE" : "FALSE") << "\n";

  int A2[] = {3, 1, 5, 2, 5};
  std::vector<int> v2(A2, A2+sizeof(A2)/sizeof(int));
  n = s.maxProfit(v2);
  std::cout << "{3, 1, 5, 2, 5} maxProfit = 4 " << (n == 4 ? "TRUE" : "FALSE") << "\n";

  return 0;
}
