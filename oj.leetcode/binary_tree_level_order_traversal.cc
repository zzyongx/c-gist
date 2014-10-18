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
  void levelN(TreeNode *root, std::vector< std::vector<int> > &vv, size_t i) {
    if (!root) return;
    if (vv.size() < i+1) vv.resize(i+1);
    vv[i].push_back(root->val);

    levelN(root->left, vv, i+1);
    levelN(root->right, vv, i+1);
  }
    
  std::vector< std::vector<int> > levelOrder(TreeNode *root) {
    std::vector< std::vector<int> > vv;
    if (!root) return vv;
    levelN(root, vv, 0);
    return vv;
  }
};

TreeNode *tree()
{
  TreeNode *n0 = new TreeNode(3);
  TreeNode *n1 = new TreeNode(9);
  TreeNode *n2 = new TreeNode(20);
  TreeNode *n3 = new TreeNode(15);
  TreeNode *n4 = new TreeNode(7);

  n0->left = n1;
  n0->right = n2;

  n2->left = n3;
  n2->right = n4;

  return n0;
}

int main()
{
  Solution s;
  std::vector< std::vector<int> > vv;

  vv = s.levelOrder(tree());
  if (vv[0][0] == 3 &&
      vv[1][0] == 9 && vv[1][1] == 20 &&
      vv[2][0] == 15 && vv[2][1] == 7) {
    std::cout << "{3,9,20,#,#,15,7} level order traversal TRUE\n";
  } else {
    std::cout << "{3,9,20,#,#,15,7} level order traversal FALSE\n";
  }
  return 0;
}
