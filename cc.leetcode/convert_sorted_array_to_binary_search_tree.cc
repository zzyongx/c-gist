#include <iostream>
#include <vector>

struct TreeNode {
  int val;
  TreeNode *left;
  TreeNode *right;
  TreeNode(int x) : val(x), left(NULL), right(NULL) {}
};

class Solution {
public:
  TreeNode *sortedArrayToBST(std::vector<int> &num, size_t start, size_t end) {
    if (end - start > 2) {
      size_t mid = start + (end - start) / 2;
      TreeNode *root = new TreeNode(num[mid]);
      root->left = sortedArrayToBST(num, start, mid);
      root->right = sortedArrayToBST(num, mid+1, end);
      return root;
    } else if (end - start == 2) {
      TreeNode *n1 = new TreeNode(num[start]);
      TreeNode *n2 = new TreeNode(num[start+1]);
      if (n1->val > n2->val) {
        n1->left = n2;
      } else {
        n1->right = n2;
      }
      return n1;
    } else {
      return new TreeNode(num[start]);
    }
  }
  
  TreeNode *sortedArrayToBST(std::vector<int> &num) {
    if (num.empty()) return NULL;
    return sortedArrayToBST(num, 0, num.size());
  }
};

static bool isBST(TreeNode *t)
{
  if (!t) return true;
  
  bool left = true, right = true;
  if (t->left) left = (t->left->val <= t->val);
  if (t->right) right = (t->right->val > t->val);

  return left && right && isBST(t->left) && isBST(t->right);
}

int main()
{
  Solution s;

  int A[] = {0, 1, 2, 3, 4, 5, 6};
  std::vector<int> num(A, A + 7);
  TreeNode *t = s.sortedArrayToBST(num);

  std::cout << (isBST(t) ? "TRUE" : "FALSE") << "\n";
  return 0;
}  
