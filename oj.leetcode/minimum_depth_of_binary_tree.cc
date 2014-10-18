#include <iostream>

struct TreeNode {
  int val;
  TreeNode *left;
  TreeNode *right;
  TreeNode(int x) : val(x), left(NULL), right(NULL) {}
};

class Solution {
public:
  int min(int a, int b) {
    return a < b ? a : b;
  }
  
  int minDepth(TreeNode *root) {
    if (!root) return 0;
    
    if (root->left && root->right) {
      return min(minDepth(root->left), minDepth(root->right))+1;
    } else if (root->left) {
      return minDepth(root->left) + 1;
    } else {
      return minDepth(root->right) + 1;
    }
  }
};

TreeNode *tree()
{
  TreeNode *n0 = new TreeNode(0);
  TreeNode *n1 = new TreeNode(1);
  TreeNode *n2 = new TreeNode(2);
  TreeNode *n3 = new TreeNode(3);
  TreeNode *n4 = new TreeNode(4);
  TreeNode *n5 = new TreeNode(5);

  n0->left = n1;
  n0->right = n2;

  n2->left = n3;
  n2->right = n4;

  n3->left = n5;

  return n0;  
}

TreeNode *tree1()
{
  TreeNode *n0 = new TreeNode(0);
  TreeNode *n1 = new TreeNode(1);

  n0->left = n1;
  return n0;
}

int main()
{
  Solution s;
  int n;

  n = s.minDepth(tree());
  std::cout << "{0,1,2,#,#,3,4,5} min depth 2 " << (n == 2 ? "TRUE" : "FALSE") << "\n";

  n = s.minDepth(tree1());
  std::cout << "{0,1} min depth 2 " << (n == 2 ? "TRUE" : "FALSE") << "\n";

  return 0;
}
