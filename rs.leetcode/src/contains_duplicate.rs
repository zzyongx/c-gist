pub struct Solution();

use std::collections::HashSet;
impl Solution {
    pub fn contains_duplicate_1(nums: Vec<i32>) -> bool {
        for i in 0..nums.len() {
            for j in i + 1..nums.len() {
                if nums[i] == nums[j] {
                    return true;
                }
            }
        }
        return false;
    }

    pub fn contains_duplicate_2(nums: Vec<i32>) -> bool {
        let mut set: HashSet<i32> = HashSet::new();
        for num in nums {
            if set.contains(&num) {
                return true;
            } else {
                set.insert(num);
            }
        }
        return false;
    }

    pub fn contains_duplicate(nums: Vec<i32>) -> bool {
        let mut nums = nums.clone();
        nums.sort();
        for i in 1..nums.len() {
            if nums[i - 1] == nums[i] {
                return true;
            }
        }
        return false;
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn contains_duplicate() {
        let nums = vec![1, 2, 3, 1];
        let v = Solution::contains_duplicate(nums);
        assert_eq!(v, true);

        let nums = vec![1, 2, 3, 4];
        let v = Solution::contains_duplicate(nums);
        assert_eq!(v, false);

        let nums = vec![1, 1, 1, 3, 3, 4, 3, 2, 4, 2];
        let v = Solution::contains_duplicate(nums);
        assert_eq!(v, true);

        let nums = vec![];
        let v = Solution::contains_duplicate(nums);
        assert_eq!(v, false);
    }
}
