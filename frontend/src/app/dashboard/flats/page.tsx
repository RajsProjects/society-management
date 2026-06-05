'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { flatsApi } from '@/lib/api';
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
import { toast } from 'sonner';
import { Plus, Search, Home, Loader2 } from 'lucide-react';
import type { FlatResponse, PagedResponse } from '@/lib/types';

export default function FlatsPage() {
  const { isAdmin } = useAuth();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  const { data, isLoading, mutate } = useSWR<PagedResponse<FlatResponse>>(
    ['flats', page],
    () => flatsApi.getAll(page, 10)
  );

  const handleCreateFlat = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsCreating(true);
    const formData = new FormData(e.currentTarget);
    
    try {
      await flatsApi.create({
        flatNumber: formData.get('flatNumber') as string,
        floor: parseInt(formData.get('floor') as string),
        block: formData.get('block') as string,
        type: formData.get('type') as string,
        area: parseFloat(formData.get('area') as string),
      });
      toast.success('Flat created successfully');
      setIsCreateOpen(false);
      mutate();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to create flat');
    } finally {
      setIsCreating(false);
    }
  };

  const filteredFlats = data?.content?.filter((flat) =>
    flat.flatNumber.toLowerCase().includes(search.toLowerCase()) ||
    flat.block?.toLowerCase().includes(search.toLowerCase()) ||
    flat.ownerName?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Flats</h1>
          <p className="text-muted-foreground">Manage society flats and units</p>
        </div>
        {isAdmin && (
          <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="mr-2 h-4 w-4" />
                Add Flat
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Add New Flat</DialogTitle>
                <DialogDescription>Enter the details for the new flat unit.</DialogDescription>
              </DialogHeader>
              <form onSubmit={handleCreateFlat} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="flatNumber">Flat Number</Label>
                    <Input id="flatNumber" name="flatNumber" placeholder="A-101" required />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="block">Block</Label>
                    <Input id="block" name="block" placeholder="A" required />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="floor">Floor</Label>
                    <Input id="floor" name="floor" type="number" placeholder="1" required />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="type">Type</Label>
                    <Input id="type" name="type" placeholder="2BHK" required />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="area">Area (sq ft)</Label>
                  <Input id="area" name="area" type="number" placeholder="1200" required />
                </div>
                <div className="flex justify-end gap-2">
                  <Button type="button" variant="outline" onClick={() => setIsCreateOpen(false)}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={isCreating}>
                    {isCreating ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                    Create Flat
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
              <CardTitle className="text-foreground">All Flats</CardTitle>
              <CardDescription>
                {data?.totalElements || 0} flats in the society
              </CardDescription>
            </div>
            <div className="relative w-full sm:w-64">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search flats..."
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
          ) : filteredFlats && filteredFlats.length > 0 ? (
            <>
              <div className="rounded-lg border border-border overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-muted/50">
                      <TableHead>Flat</TableHead>
                      <TableHead>Block</TableHead>
                      <TableHead>Floor</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Area</TableHead>
                      <TableHead>Owner</TableHead>
                      <TableHead>Status</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredFlats.map((flat) => (
                      <TableRow key={flat.id}>
                        <TableCell className="font-medium">
                          <div className="flex items-center gap-2">
                            <Home className="h-4 w-4 text-muted-foreground" />
                            {flat.flatNumber}
                          </div>
                        </TableCell>
                        <TableCell>{flat.block}</TableCell>
                        <TableCell>{flat.floor}</TableCell>
                        <TableCell>{flat.type}</TableCell>
                        <TableCell>{flat.area} sq ft</TableCell>
                        <TableCell>
                          {flat.ownerName || <span className="text-muted-foreground">-</span>}
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant={flat.occupied ? 'default' : 'secondary'}
                            className={flat.occupied ? 'bg-success/10 text-success border-success/20' : ''}
                          >
                            {flat.occupied ? 'Occupied' : 'Vacant'}
                          </Badge>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {data && data.totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Page {page + 1} of {data.totalPages}
                  </p>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={data.first}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => p + 1)}
                      disabled={data.last}
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="flex flex-col items-center justify-center py-12">
              <Home className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No flats found</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
