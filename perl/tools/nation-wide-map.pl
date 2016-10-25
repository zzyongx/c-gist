#!/usr/bin/perl

use strict;
use warnings;
use utf8;
use Data::Dumper;
use Encode qw /decode_utf8 encode_utf8/;
use JSON::PP 'encode_json';

my $SP = decode_utf8("　");

my @map;
my $ref2 = undef;
my $ref3 = undef;

my @lines;

while (my $line = <>) {
  chomp($line);
  push @lines, decode_utf8($line);
  $line =~ s/\d+\s+//g;

  $line = decode_utf8($line);
  $line =~ s/$SP/ /g;
  # $line = encode_utf8($line);

  $line =~ /(\s+)([^\s]+)/;
  my $space = length($1);

  if ($space == 1) {
    $ref2 = [];
    push(@map, {name => $2, area => $ref2});
  } elsif ($space == 2) {
    $ref3 = [];
    push(@$ref2, {name => $2, area => $ref3});
  } else {
    push(@$ref3, $2);
  }
}

# print Dumper(\@map);
print encode_json(\@map);
