'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { billsApi } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { toast } from 'sonner';
import { Search, Receipt, Loader2, Plus, CreditCard } from 'lucide-react';
import type { MaintenanceBill, PagedResponse } from '@/lib/types';

const months = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

export default function BillingPage() {
  const { isAdmin, hasRole, user } = useAuth();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [isGenerateOpen, setIsGenerateOpen] = useState(false);
  const [isPayOpen, setIsPayOpen] = useState(false);
  const [selectedBill, setSelectedBill] = useState<MaintenanceBill | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const isAccountant = hasRole('ACCOUNTANT');
  const canManageBills = isAdmin || isAccountant;

  const { data: allBills, isLoading: allBillsLoading, mutate: mutateAllBills } = useSWR<PagedResponse<MaintenanceBill>>(
    canManageBills ? ['bills', page] : null,
    () => billsApi.getAll(page, 10)
  );

  const { data: myBills, isLoading: myBillsLoading, mutate: mutateMyBills } = useSWR<MaintenanceBill[]>(
    !canManageBills ? 'my-bills' : null,
    () => billsApi.getMyBills()
  );

  const isLoading = allBillsLoading || myBillsLoading;
  const bills = canManageBills ? allBills?.content : myBills;

  const handleGenerateBills = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsProcessing(true);
    const formData = new FormData(e.currentTarget);
    
    try {
      await billsApi.generateMonthly(
        formData.get('month') as string,
        parseInt(formData.get('year') as string)
      );
      toast.success('Bills generated successfully');
      setIsGenerateOpen(false);
      mutateAllBills();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to generate bills');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleMarkPaid = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedBill) return;
    setIsProcessing(true);
    const formData = new FormData(e.currentTarget);
    
    try {
      await billsApi.markPaid(selectedBill.id, parseFloat(formData.get('amount') as string));
      toast.success('Payment recorded successfully');
      setIsPayOpen(false);
      setSelectedBill(null);
      mutateAllBills();
      mutateMyBills();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to record payment');
    } finally {
      setIsProcessing(false);
    }
  };

  const filteredBills = bills?.filter((bill) =>
    bill.flatNumber.toLowerCase().includes(search.toLowerCase()) ||
    bill.billMonth.toLowerCase().includes(search.toLowerCase())
  );

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const statusColors: Record<string, string> = {
    PENDING: 'bg-warning/10 text-warning border-warning/20',
    PAID: 'bg-success/10 text-success border-success/20',
    OVERDUE: 'bg-destructive/10 text-destructive border-destructive/20',
    PARTIALLY_PAID: 'bg-chart-2/10 text-chart-2 border-chart-2/20',
  };

  const currentYear = new Date().getFullYear();
  const years = [currentYear - 1, currentYear, currentYear + 1];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Billing</h1>
          <p className="text-muted-foreground">
            {canManageBills ? 'Manage maintenance bills' : 'View your maintenance bills'}
          </p>
        </div>
        {canManageBills && (
          <Dialog open={isGenerateOpen} onOpenChange={setIsGenerateOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="mr-2 h-4 w-4" />
                Generate Bills
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Generate Monthly Bills</DialogTitle>
                <DialogDescription>Generate maintenance bills for all flats.</DialogDescription>
              </DialogHeader>
              <form onSubmit={handleGenerateBills} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="month">Month</Label>
                    <Select name="month" required>
                      <SelectTrigger>
                        <SelectValue placeholder="Select month" />
                      </SelectTrigger>
                      <SelectContent>
                        {months.map((month) => (
                          <SelectItem key={month} value={month.toUpperCase()}>
                            {month}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="year">Year</Label>
                    <Select name="year" required>
                      <SelectTrigger>
                        <SelectValue placeholder="Select year" />
                      </SelectTrigger>
                      <SelectContent>
                        {years.map((year) => (
                          <SelectItem key={year} value={year.toString()}>
                            {year}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                <div className="flex justify-end gap-2">
                  <Button type="button" variant="outline" onClick={() => setIsGenerateOpen(false)}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={isProcessing}>
                    {isProcessing ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                    Generate
                  </Button>
                </div>
              </form>
            </DialogContent>
          </Dialog>
        )}
      </div>

      <Card className="bg-card border-border">
        <CardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center gap-4">
            <div className="flex-1">
              <CardTitle className="text-foreground">
                {canManageBills ? 'All Bills' : 'My Bills'}
              </CardTitle>
              <CardDescription>
                {canManageBills
                  ? `${allBills?.totalElements || 0} bills in total`
                  : `${myBills?.length || 0} bills for your flat`}
              </CardDescription>
            </div>
            <div className="relative w-full sm:w-64">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search bills..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9 bg-input"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : filteredBills && filteredBills.length > 0 ? (
            <>
              <div className="rounded-lg border border-border overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-muted/50">
                      <TableHead>Flat</TableHead>
                      <TableHead>Period</TableHead>
                      <TableHead>Amount</TableHead>
                      <TableHead>Paid</TableHead>
                      <TableHead>Due Date</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="w-12"></TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredBills.map((bill) => (
                      <TableRow key={bill.id}>
                        <TableCell className="font-medium">{bill.flatNumber}</TableCell>
                        <TableCell>
                          {bill.billMonth} {bill.billYear}
                        </TableCell>
                        <TableCell>{formatCurrency(bill.amount)}</TableCell>
                        <TableCell>{formatCurrency(bill.paidAmount)}</TableCell>
                        <TableCell>
                          {new Date(bill.dueDate).toLocaleDateString('en-IN')}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className={statusColors[bill.status] || ''}>
                            {bill.status.replace('_', ' ')}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          {bill.status !== 'PAID' && (
                            <Dialog open={isPayOpen && selectedBill?.id === bill.id} onOpenChange={(open) => {
                              setIsPayOpen(open);
                              if (!open) setSelectedBill(null);
                            }}>
                              <DialogTrigger asChild>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => setSelectedBill(bill)}
                                >
                                  <CreditCard className="h-4 w-4" />
                                </Button>
                              </DialogTrigger>
                              <DialogContent>
                                <DialogHeader>
                                  <DialogTitle>Record Payment</DialogTitle>
                                  <DialogDescription>
                                    Record payment for {bill.flatNumber} - {bill.billMonth} {bill.billYear}
                                  </DialogDescription>
                                </DialogHeader>
                                <form onSubmit={handleMarkPaid} className="space-y-4">
                                  <div className="p-4 rounded-lg bg-muted/50 space-y-2">
                                    <div className="flex justify-between text-sm">
                                      <span className="text-muted-foreground">Total Amount</span>
                                      <span className="font-medium">{formatCurrency(bill.amount)}</span>
                                    </div>
                                    <div className="flex justify-between text-sm">
                                      <span className="text-muted-foreground">Already Paid</span>
                                      <span className="font-medium">{formatCurrency(bill.paidAmount)}</span>
                                    </div>
                                    <div className="flex justify-between text-sm font-medium">
                                      <span>Remaining</span>
                                      <span className="text-primary">{formatCurrency(bill.amount - bill.paidAmount)}</span>
                                    </div>
                                  </div>
                                  <div className="space-y-2">
                                    <Label htmlFor="amount">Payment Amount</Label>
                                    <Input
                                      id="amount"
                                      name="amount"
                                      type="number"
                                      defaultValue={bill.amount - bill.paidAmount}
                                      max={bill.amount - bill.paidAmount}
                                      required
                                    />
                                  </div>
                                  <div className="flex justify-end gap-2">
                                    <Button type="button" variant="outline" onClick={() => setIsPayOpen(false)}>
                                      Cancel
                                    </Button>
                                    <Button type="submit" disabled={isProcessing}>
                                      {isProcessing ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                                      Record Payment
                                    </Button>
                                  </div>
                                </form>
                              </DialogContent>
                            </Dialog>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {canManageBills && allBills && allBills.totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Page {page + 1} of {allBills.totalPages}
                  </p>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={allBills.first}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => p + 1)}
                      disabled={allBills.last}
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="flex flex-col items-center justify-center py-12">
              <Receipt className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No bills found</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
