#include <iostream>

struct TreeNode {
  int val;
  TreeNode *left;
  TreeNode *right;
  TreeNode(int x) : val(x), left(NULL), right(NULL) {}
};

class Solution {
public:
  bool isEqual(TreeNode *left, TreeNode *right) {
    if (!left && !right) return true;
    return left && right && left->val == right->val &&
      isEqual(left->left, right->right) &&
      isEqual(left->right, right->left);
  }
  
  bool isSymmetric(TreeNode *root) {
    if (!root) return true;
    return isEqual(root->left, root->right);
  }
};

int main()
{
  TreeNode n1(1);
  TreeNode n2(2);
  TreeNode n3(2);
  TreeNode n4(3);
  TreeNode n5(4);
  TreeNode n6(4);
  TreeNode n7(3);

  n1.left = &n2;
  n1.right = &n3;

  n2.left = &n4;
  n2.right = &n5;

  n3.left = &n6;
  n3.right = &n7;

  Solution s;
  bool b;

  b = s.isSymmetric(&n1);
  std::cout << "{1, 2, 2, 3, 4, 4, 3} is symmetric " << (b ? "TRUE" : "FALSE") << "\n";

  return 0;
}
